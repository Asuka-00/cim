package com.scaffold.share.cache;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * QueryCacheInterceptor 透明查询缓存（MyBatis Executor 插件）。
 * <p>
 * <b>读</b>：对 SELECT 以“最终 SQL + 参数 + 涉及各表版本号”为 key 缓存 {@code List} 结果。<br>
 * <b>写</b>：insert/update/delete 后递增涉及表的版本号，旧版本缓存立即不可达。<br>
 * 必须注册在 MybatisPlusInterceptor 之前（即更内层），这样拿到的是已叠加租户条件与分页的最终 SQL，
 * 不同租户、不同页天然落在不同 key。
 * <p>
 * 一致性约定：
 * <ul>
 * <li>事务内写过的表，事务内读绕过缓存（读到自己的写）；提交后才递增版本号，回滚不递增。</li>
 * <li>缓存层任何异常都降级为直接查库，连续失败触发熔断。</li>
 * <li>绕过 MyBatis 的写入（手工 SQL、迁移脚本）只靠 TTL 兜底，或调用 {@link QueryCacheInvalidator}。</li>
 * </ul>
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-15
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query", args = { MappedStatement.class, Object.class,
                RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class }),
        @Signature(type = Executor.class, method = "query", args = { MappedStatement.class, Object.class,
                RowBounds.class, ResultHandler.class }),
        @Signature(type = Executor.class, method = "update", args = { MappedStatement.class, Object.class })
})
public class QueryCacheInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(QueryCacheInterceptor.class);

    /** 非确定性 SQL（结果随时间/随机变化）不缓存；FOR UPDATE 需要真实加锁 */
    private static final Pattern UNCACHEABLE_SQL = Pattern.compile(
            "(?i)\\b(now\\s*\\(|current_timestamp|current_date|sysdate|getdate\\s*\\(|rand\\s*\\(|random\\s*\\(|uuid\\s*\\(|newid\\s*\\()"
                    + "|\\bfor\\s+update\\b");

    private static final int LOCK_STRIPES = 64;
    private static final Object DIRTY_RESOURCE = new Object();

    private final QueryCacheProperties properties;
    private final QueryCacheBackend backend;
    private final QueryCacheSerializer serializer;
    private final SqlTableResolver tableResolver;
    private final MeterRegistry meterRegistry;
    private final List<Pattern> excludes;
    private final List<Pattern> includes;
    private final ReentrantLock[] locks = new ReentrantLock[LOCK_STRIPES];
    private final Map<String, Policy> policies = new ConcurrentHashMap<>();
    /** 反序列化失败过的语句：之后不再尝试缓存，避免反复失败 */
    private final Set<String> brokenStatements = ConcurrentHashMap.newKeySet();
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicLong breakerOpenUntil = new AtomicLong();

    public QueryCacheInterceptor(QueryCacheProperties properties, QueryCacheBackend backend,
            QueryCacheSerializer serializer, SqlTableResolver tableResolver, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.backend = backend;
        this.serializer = serializer;
        this.tableResolver = tableResolver;
        this.meterRegistry = meterRegistry;
        this.excludes = compile(properties.getExcludeTables());
        this.includes = compile(properties.getIncludeTables());
        for (int i = 0; i < LOCK_STRIPES; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        if ("update".equals(invocation.getMethod().getName())) {
            return interceptUpdate(invocation, ms);
        }
        return interceptQuery(invocation, ms);
    }

    // ==================== 写：递增表版本号 ====================

    private Object interceptUpdate(Invocation invocation, MappedStatement ms) throws Throwable {
        Object result = invocation.proceed();
        if (!properties.isEnabled()) {
            return result;
        }
        try {
            Set<String> tables = tableResolver.tables(ms.getBoundSql(invocation.getArgs()[1]).getSql());
            markWritten(tables);
        } catch (RuntimeException ex) {
            log.warn("Query cache invalidation failed, statement={}: {}", ms.getId(), ex.toString());
        }
        return result;
    }

    /**
     * tables 为 null 表示无法解析，保守地让全部缓存失效
     */
    private void markWritten(Set<String> tables) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            bumpNow(tables);
            return;
        }
        @SuppressWarnings("unchecked")
        Set<String> dirty = (Set<String>) TransactionSynchronizationManager.getResource(DIRTY_RESOURCE);
        if (dirty == null) {
            Set<String> fresh = new HashSet<>();
            TransactionSynchronizationManager.bindResource(DIRTY_RESOURCE, fresh);
            TransactionSynchronizationManager.registerSynchronization(new DirtyTablesSynchronization(fresh));
            dirty = fresh;
        }
        if (tables == null) {
            dirty.add("*");
        } else {
            dirty.addAll(tables);
        }
    }

    private void bumpNow(Set<String> tables) {
        try {
            if (tables == null) {
                backend.bumpAll();
            } else {
                backend.bump(tables);
            }
        } catch (RuntimeException ex) {
            recordFailure(ex);
        }
    }

    private final class DirtyTablesSynchronization implements TransactionSynchronization {

        private final Set<String> dirty;

        private DirtyTablesSynchronization(Set<String> dirty) {
            this.dirty = dirty;
        }

        @Override
        public void afterCommit() {
            if (dirty.contains("*")) {
                bumpNow(null);
            } else {
                bumpNow(dirty);
            }
        }

        @Override
        public void afterCompletion(int status) {
            if (TransactionSynchronizationManager.hasResource(DIRTY_RESOURCE)) {
                TransactionSynchronizationManager.unbindResource(DIRTY_RESOURCE);
            }
        }
    }

    private static boolean writtenInCurrentTransaction(Set<String> tables) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Set<String> dirty = (Set<String>) TransactionSynchronizationManager.getResource(DIRTY_RESOURCE);
        if (dirty == null) {
            return false;
        }
        if (dirty.contains("*")) {
            return true;
        }
        for (String table : tables) {
            if (dirty.contains(table)) {
                return true;
            }
        }
        return false;
    }

    // ==================== 读：缓存 ====================

    private Object interceptQuery(Invocation invocation, MappedStatement ms) throws Throwable {
        Object[] args = invocation.getArgs();
        if (!properties.isEnabled() || ms.getSqlCommandType() != SqlCommandType.SELECT
                || args[3] != Executor.NO_RESULT_HANDLER || System.currentTimeMillis() < breakerOpenUntil.get()
                || brokenStatements.contains(ms.getId())) {
            return invocation.proceed();
        }
        Policy policy = policies.computeIfAbsent(ms.getId(), Policy::of);
        if (policy.noCache) {
            return bypass(invocation, "annotated");
        }
        BoundSql boundSql = args.length == 6 ? (BoundSql) args[5] : ms.getBoundSql(args[1]);
        String sql = boundSql.getSql();
        if (UNCACHEABLE_SQL.matcher(sql).find()) {
            return bypass(invocation, "uncacheable");
        }
        Set<String> tables = tableResolver.tables(sql);
        if (tables == null || !cacheable(tables)) {
            return bypass(invocation, "table");
        }
        if (writtenInCurrentTransaction(tables)) {
            return bypass(invocation, "dirty");
        }

        String dataKey;
        try {
            Executor executor = (Executor) invocation.getTarget();
            CacheKey cacheKey = executor.createCacheKey(ms, args[1], (RowBounds) args[2], boundSql);
            List<String> versions = backend.versions(new ArrayList<>(tables));
            dataKey = properties.getKeyPrefix() + ":d:" + sha256(cacheKey + "|" + String.join(",", versions));
            String cached = backend.get(dataKey);
            if (cached != null) {
                List<Object> rows = tryRead(ms, dataKey, cached);
                if (rows != null) {
                    consecutiveFailures.set(0);
                    count("hit");
                    return rows;
                }
            }
        } catch (RuntimeException ex) {
            recordFailure(ex);
            return invocation.proceed();
        }

        // 未命中：同 key 单飞回源，其它线程等待后复查
        ReentrantLock lock = locks[(dataKey.hashCode() & Integer.MAX_VALUE) % LOCK_STRIPES];
        boolean locked = false;
        try {
            locked = lock.tryLock(properties.getLockWait().toMillis(), TimeUnit.MILLISECONDS);
            if (locked) {
                String again = safeGet(dataKey);
                if (again != null) {
                    List<Object> rows = tryRead(ms, dataKey, again);
                    if (rows != null) {
                        count("hit");
                        return rows;
                    }
                }
            }
            Object result = invocation.proceed();
            store(ms, policy, tables, dataKey, result);
            count("miss");
            return result;
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    private Object bypass(Invocation invocation, String reason) throws Throwable {
        count("bypass");
        if (log.isTraceEnabled()) {
            log.trace("Query cache bypass: {}", reason);
        }
        return invocation.proceed();
    }

    private boolean cacheable(Set<String> tables) {
        for (String table : tables) {
            if (matchesAny(excludes, table)) {
                return false;
            }
            if (!includes.isEmpty() && !matchesAny(includes, table)) {
                return false;
            }
        }
        return true;
    }

    private String safeGet(String key) {
        try {
            return backend.get(key);
        } catch (RuntimeException ex) {
            recordFailure(ex);
            return null;
        }
    }

    private List<Object> tryRead(MappedStatement ms, String dataKey, String cached) {
        try {
            return serializer.read(cached);
        } catch (Exception ex) {
            log.warn("Query cache entry unreadable, statement disabled: {} ({})", ms.getId(), ex.toString());
            brokenStatements.add(ms.getId());
            try {
                backend.delete(dataKey);
            } catch (RuntimeException ignore) {
                // 已降级，忽略
            }
            return null;
        }
    }

    private void store(MappedStatement ms, Policy policy, Set<String> tables, String dataKey, Object result) {
        if (!(result instanceof List<?> rows) || rows.size() > properties.getMaxRows()) {
            return;
        }
        try {
            String json = serializer.write(rows);
            backend.put(dataKey, json, jitter(policy.ttl(properties, tables)));
            consecutiveFailures.set(0);
        } catch (RuntimeException ex) {
            recordFailure(ex);
        } catch (Exception ex) {
            log.warn("Query cache serialize failed, statement disabled: {} ({})", ms.getId(), ex.toString());
            brokenStatements.add(ms.getId());
        }
    }

    private void recordFailure(RuntimeException ex) {
        count("error");
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= properties.getBreakerThreshold()) {
            breakerOpenUntil.set(System.currentTimeMillis() + properties.getBreakerOpen().toMillis());
            consecutiveFailures.set(0);
            log.warn("Query cache circuit opened for {}: {}", properties.getBreakerOpen(), ex.toString());
        } else {
            log.debug("Query cache backend error: {}", ex.toString());
        }
    }

    private void count(String result) {
        if (meterRegistry != null) {
            meterRegistry.counter("app.cache.query", "result", result).increment();
        }
    }

    /** ±10% 抖动，避免同批缓存同一时刻集中过期 */
    private static Duration jitter(Duration ttl) {
        long millis = ttl.toMillis();
        long delta = millis / 10;
        if (delta <= 0) {
            return ttl;
        }
        return Duration.ofMillis(millis - delta + ThreadLocalRandom.current().nextLong(delta * 2 + 1));
    }

    private static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static List<Pattern> compile(List<String> globs) {
        List<Pattern> patterns = new ArrayList<>();
        for (String glob : globs) {
            if (glob == null || glob.isBlank()) {
                continue;
            }
            String regex = Pattern.quote(glob.trim().toLowerCase()).replace("*", "\\E.*\\Q");
            patterns.add(Pattern.compile(regex));
        }
        return patterns;
    }

    private static boolean matchesAny(List<Pattern> patterns, String table) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(table).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 语句级策略：由 Mapper 方法 / 接口上的 {@link NoCache}、{@link CacheTtl} 决定
     */
    private record Policy(boolean noCache, Duration ttlOverride) {

        static Policy of(String statementId) {
            int dot = statementId.lastIndexOf('.');
            if (dot < 0) {
                return new Policy(false, null);
            }
            try {
                Class<?> mapper = Class.forName(statementId.substring(0, dot), false,
                        Thread.currentThread().getContextClassLoader() != null
                                ? Thread.currentThread().getContextClassLoader()
                                : QueryCacheInterceptor.class.getClassLoader());
                String methodName = statementId.substring(dot + 1);
                boolean noCache = mapper.isAnnotationPresent(NoCache.class);
                CacheTtl ttl = mapper.getAnnotation(CacheTtl.class);
                for (Method method : mapper.getMethods()) {
                    if (!method.getName().equals(methodName)) {
                        continue;
                    }
                    if (method.isAnnotationPresent(NoCache.class)) {
                        noCache = true;
                    }
                    if (method.isAnnotationPresent(CacheTtl.class)) {
                        ttl = method.getAnnotation(CacheTtl.class);
                    }
                }
                return new Policy(noCache, ttl == null ? null : Duration.ofSeconds(ttl.seconds()));
            } catch (ClassNotFoundException | LinkageError ex) {
                return new Policy(false, null);
            }
        }

        Duration ttl(QueryCacheProperties properties, Set<String> tables) {
            if (ttlOverride != null) {
                return ttlOverride;
            }
            Duration result = null;
            for (String table : tables) {
                Duration configured = properties.getTableTtl().get(table);
                if (configured != null && (result == null || configured.compareTo(result) < 0)) {
                    result = configured;
                }
            }
            return result != null ? result : properties.getDefaultTtl();
        }
    }
}
