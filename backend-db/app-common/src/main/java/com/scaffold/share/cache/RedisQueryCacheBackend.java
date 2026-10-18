package com.scaffold.share.cache;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * RedisQueryCacheBackend 基于 Redis 的查询缓存存储。
 * <p>
 * 版本号初值取当前毫秒时间戳，并在 key 缺失（被淘汰/清库）时重新以时间戳起算，
 * 保证版本号不会回退到旧值而让陈旧缓存“复活”。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-15
 */
public class RedisQueryCacheBackend implements QueryCacheBackend {

    private static final String GLOBAL = "*";

    /** key 存在则 INCR，否则以传入的毫秒时间戳起算 */
    private static final DefaultRedisScript<Long> BUMP_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('EXISTS', KEYS[1]) == 1 then return redis.call('INCR', KEYS[1]) end "
                    + "redis.call('SET', KEYS[1], ARGV[1]) return tonumber(ARGV[1])",
            Long.class);

    private final StringRedisTemplate redis;
    private final String prefix;

    public RedisQueryCacheBackend(StringRedisTemplate redis, String keyPrefix) {
        this.redis = redis;
        this.prefix = keyPrefix;
    }

    @Override
    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        redis.opsForValue().set(key, value, ttl);
    }

    @Override
    public void delete(String key) {
        redis.delete(key);
    }

    @Override
    public List<String> versions(List<String> tables) {
        List<String> keys = new ArrayList<>(tables.size() + 1);
        keys.add(versionKey(GLOBAL));
        for (String table : tables) {
            keys.add(versionKey(table));
        }
        List<String> values = redis.opsForValue().multiGet(keys);
        List<String> result = new ArrayList<>(keys.size());
        for (int i = 0; i < keys.size(); i++) {
            String value = values == null ? null : values.get(i);
            if (value == null) {
                redis.opsForValue().setIfAbsent(keys.get(i), String.valueOf(System.currentTimeMillis()));
                value = redis.opsForValue().get(keys.get(i));
            }
            result.add(value == null ? "0" : value);
        }
        return result;
    }

    @Override
    public void bump(Collection<String> tables) {
        for (String table : tables) {
            bumpKey(versionKey(table));
        }
    }

    @Override
    public void bumpAll() {
        bumpKey(versionKey(GLOBAL));
    }

    private void bumpKey(String key) {
        redis.execute(BUMP_SCRIPT, List.of(key), String.valueOf(System.currentTimeMillis()));
    }

    private String versionKey(String table) {
        return prefix + ":v:" + table;
    }
}
