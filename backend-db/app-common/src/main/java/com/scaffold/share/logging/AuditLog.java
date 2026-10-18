package com.scaffold.share.logging;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AuditLog注解类型
 * <p>
 * 标记需要记录接口入参、出参和异常信息的Controller方法。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-23
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /**
     * 操作描述
     *
     * @return 操作名称
     */
    String operation() default "";

    /**
     * 是否记录请求入参
     *
     * @return true: 记录, false: 不记录
     */
    boolean logRequest() default true;

    /**
     * 是否记录响应出参
     *
     * @return true: 记录, false: 不记录
     */
    boolean logResponse() default true;

    /**
     * 是否对敏感字段脱敏
     *
     * @return true: 脱敏, false: 不脱敏
     */
    boolean mask() default true;

    /**
     * 是否写入操作日志表
     *
     * @return true: 写入DB, false: 仅输出日志
     */
    boolean writeDb() default true;

    /**
     * 单个审计payload最大长度
     *
     * @return 最大字符数
     */
    int maxPayloadLength() default 4096;
}
