package com.scaffold.share.cache;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * NoCache 声明某个 Mapper（或其方法）的查询不进入透明查询缓存。
 * <p>
 * 默认所有 SELECT 都会被缓存；仅当结果对实时性要求极高时才需要标注。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-15
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface NoCache {
}
