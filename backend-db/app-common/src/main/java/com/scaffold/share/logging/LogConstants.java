package com.scaffold.share.logging;

/**
 * LogConstants类
 * <p>
 * 负责处理LogConstants相关核心业务能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
public final class LogConstants {

    private LogConstants() {
    }

    /** traceId */
    public static final String TRACE_ID = "traceId";

    /** 用户代码 */
    public static final String USER_CODE = "userName";

    /** 租户ID */
    public static final String TENANT_ID = "tenantId";

    /** 操作类型 */
    public static final String OPERATION = "operation";

    /** 执行耗时(毫秒) */
    public static final String DURATION = "duration";

    /** 操作是否成功 */
    public static final String SUCCESS = "success";

    /** 请求URI */
    public static final String REQUEST_URI = "requestUri";

    /** 请求方法 */
    public static final String REQUEST_METHOD = "requestMethod";

    /** 请求参数 */
    public static final String REQUEST_PARAMS = "requestParams";

    /** 响应码 */
    public static final String RESPONSE_CODE = "responseCode";

    /** 错误消息 */
    public static final String ERROR_MSG = "errorMsg";

    /** 异常堆栈 */
    public static final String STACK_TRACE = "stackTrace";

    /** 类名 */
    public static final String CLASS_NAME = "className";

    /** 方法名 */
    public static final String METHOD_NAME = "methodName";

    /** 请求IP */
    public static final String REQUEST_IP = "requestIp";

    /** 操作系统 */
    public static final String OS = "os";

    /** 浏览器 */
    public static final String BROWSER = "browser";

    /** 用户代理 */
    public static final String USER_AGENT = "userAgent";

    /** 业务操作成功标记 */
    public static final String SUCCESS_TRUE = "true";

    /** 业务操作失败标记 */
    public static final String SUCCESS_FALSE = "false";
}
