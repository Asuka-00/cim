package com.scaffold.share.cache;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * CacheTtl 覆盖某个 Mapper（或其方法）查询结果的缓存存活时间（秒）。
 * <p>
 * 优先级：方法 &gt; 类型 &gt; {@code app.cache.table-ttl} &gt; {@code app.cache.default-ttl}。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-15
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface CacheTtl {

    /**
     * 存活秒数
     */
    long seconds();
}
