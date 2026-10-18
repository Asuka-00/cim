package com.scaffold.share.cache;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

import org.springframework.lang.Nullable;

/**
 * QueryCacheBackend 查询缓存存储抽象。
 * <p>
 * 除键值读写外，还负责“表版本号”：每张表一个单调递增版本号，写入时递增，
 * 读缓存 key 里带上版本号，旧版本的缓存自然不可达，无需逐条删除。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-15
 */
public interface QueryCacheBackend {

    /**
     * 读取缓存值
     */
    @Nullable
    String get(String key);

    /**
     * 写入缓存值
     */
    void put(String key, String value, Duration ttl);

    /**
     * 删除缓存值
     */
    void delete(String key);

    /**
     * 读取全局版本号与各表版本号，顺序为：global、tables...
     */
    List<String> versions(List<String> tables);

    /**
     * 递增各表版本号
     */
    void bump(Collection<String> tables);

    /**
     * 递增全局版本号，使全部缓存失效
     */
    void bumpAll();
}
