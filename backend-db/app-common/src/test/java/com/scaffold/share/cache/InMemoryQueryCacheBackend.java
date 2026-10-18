package com.scaffold.share.cache;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 测试用内存后端，语义与 Redis 后端一致（不模拟 TTL）。
 */
class InMemoryQueryCacheBackend implements QueryCacheBackend {

    final Map<String, String> data = new ConcurrentHashMap<>();
    final Map<String, AtomicLong> versions = new ConcurrentHashMap<>();
    boolean failing;

    @Override
    public String get(String key) {
        check();
        return data.get(key);
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        check();
        data.put(key, value);
    }

    @Override
    public void delete(String key) {
        data.remove(key);
    }

    @Override
    public List<String> versions(List<String> tables) {
        check();
        List<String> result = new ArrayList<>();
        result.add(String.valueOf(versions.computeIfAbsent("*", k -> new AtomicLong(1)).get()));
        for (String table : tables) {
            result.add(String.valueOf(versions.computeIfAbsent(table, k -> new AtomicLong(1)).get()));
        }
        return result;
    }

    @Override
    public void bump(Collection<String> tables) {
        check();
        tables.forEach(t -> versions.computeIfAbsent(t, k -> new AtomicLong(1)).incrementAndGet());
    }

    @Override
    public void bumpAll() {
        check();
        versions.computeIfAbsent("*", k -> new AtomicLong(1)).incrementAndGet();
    }

    private void check() {
        if (failing) {
            throw new IllegalStateException("backend down");
        }
    }
}
