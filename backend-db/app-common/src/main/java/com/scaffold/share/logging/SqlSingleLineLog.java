package com.scaffold.share.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ibatis.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import net.logstash.logback.argument.StructuredArguments;

/**
 * MyBatis SQL 单行日志：聚合 Preparing / Parameters / Total|Updates 后一次输出。
 * <p>
 * 输出到专用 logger {@code SQL}（文件 sql.log），message 含可读 SQL；
 * StructuredArguments 保留 sql/params/duration 等字段便于机器检索。
 * 状态使用 ThreadLocal，适配虚拟线程并发。
 *
 * @author Kairo
 * @version 1.2.0
 * @since 2026-04-09
 */
public class SqlSingleLineLog implements Log {

    private static final String SQL_LOGGER_NAME = "SQL";
    private static final String EVENT_SQL_EXEC = "SQL_EXEC";

    private static final String SQL_KEY = "sql";
    private static final String PARAMS_KEY = "params";
    private static final String DURATION_KEY = "duration";
    private static final String COUNT_KEY = "count";
    private static final String MAPPER_KEY = "mapper";

    private static final Pattern PREPARING_PATTERN = Pattern.compile("(?i).*preparing:\\s*(.*)");
    private static final Pattern PARAMETERS_PATTERN = Pattern.compile("(?i).*parameters:\\s*(.*)");
    private static final Pattern TOTAL_PATTERN = Pattern.compile("(?i).*<==\\s*total:\\s*(\\d+).*");
    private static final Pattern UPDATES_PATTERN = Pattern.compile("(?i).*<==\\s*updates:\\s*(\\d+).*");
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");

    /** 输出通道：固定 SQL logger */
    private static final Logger SQL_LOGGER = LoggerFactory.getLogger(SQL_LOGGER_NAME);

    /** 逐条 SQL 日志总开关（logging.sql-log-enabled）；关闭后 MyBatis 不再拼装调试日志 */
    private static volatile boolean enabled = true;

    /** 用于 isDebugEnabled：与 Mapper 类绑定，受 logback 级别控制 */
    private final Logger mapperLogger;
    private final String mapperName;
    private final ThreadLocal<SqlState> stateHolder = ThreadLocal.withInitial(SqlState::new);

    public SqlSingleLineLog(String loggerName) {
        this.mapperName = loggerName;
        this.mapperLogger = LoggerFactory.getLogger(loggerName);
    }

    public SqlSingleLineLog(Class<?> clazz) {
        this.mapperName = clazz.getName();
        this.mapperLogger = LoggerFactory.getLogger(clazz);
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isDebugEnabled() {
        return enabled && mapperLogger.isDebugEnabled();
    }

    @Override
    public boolean isTraceEnabled() {
        return mapperLogger.isTraceEnabled();
    }

    @Override
    public void error(String s) {
        if (SQL_LOGGER.isErrorEnabled()) {
            flushLog(null, s);
        }
    }

    @Override
    public void error(String s, Throwable e) {
        if (SQL_LOGGER.isErrorEnabled()) {
            flushLog(e, s);
        }
    }

    @Override
    public void warn(String s) {
        if (SQL_LOGGER.isWarnEnabled()) {
            checkAndFlush(s);
        }
    }

    public void info(String s) {
        if (SQL_LOGGER.isInfoEnabled()) {
            checkAndFlush(s);
        }
    }

    @Override
    public void debug(String s) {
        if (isDebugEnabled()) {
            handleDebug(s);
        }
    }

    public void debug(String s, String s1) {
        if (isDebugEnabled()) {
            handleParameters(s1);
        }
    }

    private void handleDebug(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        if (handlePreparing(message)) {
            return;
        }
        if (handleParameters(message)) {
            return;
        }
        checkAndFlush(message);
    }

    @Override
    public void trace(String s) {
        // SQL 日志不使用 trace 通道
    }

    public void trace(String s, Throwable e) {
        // SQL 日志不使用 trace 通道
    }

    private boolean handlePreparing(String message) {
        Matcher matcher = PREPARING_PATTERN.matcher(message);
        if (!matcher.matches()) {
            return false;
        }
        SqlState state = stateHolder.get();
        state.startTime = System.currentTimeMillis();
        state.currentSql = normalizeSql(matcher.group(1).trim());
        state.currentParams = new ArrayList<>();
        state.resultCount = null;
        return true;
    }

    private boolean handleParameters(String message) {
        Matcher paramMatcher = PARAMETERS_PATTERN.matcher(message);
        if (!paramMatcher.matches()) {
            return false;
        }
        parseAndStoreParams(paramMatcher.group(1).trim());
        return true;
    }

    private void parseAndStoreParams(String rawParams) {
        if (rawParams == null || rawParams.isBlank()) {
            return;
        }
        SqlState state = stateHolder.get();
        if (state.currentParams == null) {
            state.currentParams = new ArrayList<>();
        }
        String[] parts = rawParams.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }
            String value = part.replaceAll("\\([A-Za-z0-9]+\\)$", "").trim();
            state.currentParams.add(value);
        }
    }

    private void checkAndFlush(String message) {
        Matcher totalMatcher = TOTAL_PATTERN.matcher(message);
        if (totalMatcher.matches()) {
            stateHolder.get().resultCount = Integer.parseInt(totalMatcher.group(1));
            flushLog(null, null);
            return;
        }

        Matcher updatesMatcher = UPDATES_PATTERN.matcher(message);
        if (updatesMatcher.matches()) {
            stateHolder.get().resultCount = Integer.parseInt(updatesMatcher.group(1));
            flushLog(null, null);
        }
    }

    private void flushLog(Throwable e, String errorMsg) {
        SqlState state = stateHolder.get();
        if (state.currentSql == null) {
            return;
        }

        long duration = state.startTime == null ? 0L : System.currentTimeMillis() - state.startTime;
        List<Object> params = state.currentParams == null ? List.of() : List.copyOf(state.currentParams);
        Integer resultCount = state.resultCount;
        String sql = state.currentSql;
        String executableSql = fillParameters(sql, params);
        boolean success = e == null && errorMsg == null;
        String message = buildHumanMessage(shortMapperName(), duration, resultCount, executableSql, success, e,
                errorMsg);

        try {
            if (!success) {
                SQL_LOGGER.warn(message,
                        StructuredArguments.kv("event", EVENT_SQL_EXEC),
                        StructuredArguments.kv(MAPPER_KEY, mapperName),
                        StructuredArguments.kv(SQL_KEY, sql),
                        StructuredArguments.kv(PARAMS_KEY, params),
                        StructuredArguments.kv(DURATION_KEY, duration),
                        StructuredArguments.kv("error", e != null ? e.getMessage() : errorMsg),
                        StructuredArguments.kv("success", false),
                        StructuredArguments.kv("traceId", MDC.get(LogConstants.TRACE_ID)),
                        StructuredArguments.kv("tenantId", MDC.get(LogConstants.TENANT_ID)));
            } else {
                SQL_LOGGER.info(message,
                        StructuredArguments.kv("event", EVENT_SQL_EXEC),
                        StructuredArguments.kv(MAPPER_KEY, mapperName),
                        StructuredArguments.kv(SQL_KEY, sql),
                        StructuredArguments.kv(PARAMS_KEY, params),
                        StructuredArguments.kv(DURATION_KEY, duration),
                        StructuredArguments.kv(COUNT_KEY, resultCount),
                        StructuredArguments.kv("success", true),
                        StructuredArguments.kv("traceId", MDC.get(LogConstants.TRACE_ID)),
                        StructuredArguments.kv("tenantId", MDC.get(LogConstants.TENANT_ID)));
            }
        } catch (Exception ex) {
            SQL_LOGGER.warn("SQL: {} | Params: {} | Duration: {}ms | Count: {}",
                    executableSql, params, duration, resultCount);
        } finally {
            reset();
        }
    }

    private String shortMapperName() {
        int dot = mapperName.lastIndexOf('.');
        return dot >= 0 ? mapperName.substring(dot + 1) : mapperName;
    }

    private static String buildHumanMessage(String mapper, long duration, Integer resultCount, String executableSql,
            boolean success, Throwable e, String errorMsg) {
        StringBuilder sb = new StringBuilder(executableSql.length() + 64);
        sb.append("SQL ").append(mapper).append(' ').append(duration).append("ms");
        if (resultCount != null) {
            sb.append(" rows=").append(resultCount);
        }
        if (!success) {
            sb.append(" FAIL");
            String detail = e != null ? e.getMessage() : errorMsg;
            if (detail != null && !detail.isBlank()) {
                sb.append(' ').append(detail);
            }
        }
        sb.append(" | ").append(executableSql);
        return sb.toString();
    }

    static String fillParameters(String sql, List<Object> params) {
        if (sql == null || params == null || params.isEmpty() || !sql.contains("?")) {
            return sql;
        }
        StringBuilder filled = new StringBuilder(sql.length() + params.size() * 8);
        int paramIndex = 0;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '?' && paramIndex < params.size()) {
                filled.append(formatParam(params.get(paramIndex++)));
            } else {
                filled.append(c);
            }
        }
        return filled.toString();
    }

    private static String formatParam(Object value) {
        if (value == null) {
            return "null";
        }
        String text = String.valueOf(value);
        if ("null".equalsIgnoreCase(text)) {
            return "null";
        }
        if (NUMERIC_PATTERN.matcher(text).matches()) {
            return text;
        }
        return "'" + text.replace("'", "''") + "'";
    }

    private void reset() {
        stateHolder.remove();
    }

    private static String normalizeSql(String sql) {
        if (sql == null) {
            return null;
        }
        return sql.replaceAll("\\s+", " ").trim();
    }

    private static final class SqlState {
        private String currentSql;
        private List<Object> currentParams;
        private Long startTime;
        private Integer resultCount;
    }
}
