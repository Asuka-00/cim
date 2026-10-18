package com.scaffold.share.cache;

import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * QueryCacheInvalidator 手动失效入口。
 * <p>
 * 通过 MyBatis 的写入会自动失效，无需调用。仅在绕过 MyBatis 修改数据
 * （JdbcTemplate、手工 SQL、数据修复脚本）时使用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-15
 */
public class QueryCacheInvalidator {

    private final QueryCacheBackend backend;

    public QueryCacheInvalidator(QueryCacheBackend backend) {
        this.backend = backend;
    }

    /**
     * 使涉及这些表的缓存失效
     */
    public void invalidateTables(Collection<String> tables) {
        backend.bump(tables.stream().map(t -> t.toLowerCase(Locale.ROOT)).collect(Collectors.toList()));
    }

    /**
     * 使全部查询缓存失效
     */
    public void invalidateAll() {
        backend.bumpAll();
    }
}
