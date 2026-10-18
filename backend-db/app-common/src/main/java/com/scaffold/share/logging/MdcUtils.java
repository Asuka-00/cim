package com.scaffold.share.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;

import lombok.experimental.UtilityClass;

/**
 * MdcUtils工具类
 * <p>
 * 负责提供MdcUtils相关通用辅助能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@UtilityClass
public final class MdcUtils {

    /**
     * 生成并设置traceId
     *
     * @return 生成的traceId
     */
    public static String generateTraceId() {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        MDC.put(LogConstants.TRACE_ID, traceId);
        return traceId;
    }

    /**
     * 设置traceId
     *
     * @param traceId traceId值
     */
    public static void setTraceId(String traceId) {
        if (traceId != null && !traceId.isBlank()) {
            MDC.put(LogConstants.TRACE_ID, traceId);
        }
    }

    /**
     * 获取traceId
     *
     * @return traceId值
     */
    public static String getTraceId() {
        return MDC.get(LogConstants.TRACE_ID);
    }

    /**
     * 获取当前traceId，不存在时生成并写入MDC
     *
     * @return 当前或新生成的traceId
     */
    public static String getOrCreateTraceId() {
        String traceId = getTraceId();
        if (traceId == null || traceId.isBlank()) {
            return generateTraceId();
        }
        return traceId;
    }

    /**
     * 设置用户代码
     *
     * @param userName 用户代码
     */
    public static void setUserName(String userName) {
        if (userName != null) {
            MDC.put(LogConstants.USER_CODE, userName);
        }
    }

    /**
     * 获取用户代码
     *
     * @return 用户代码
     */
    public static String getUserName() {
        return MDC.get(LogConstants.USER_CODE);
    }

    /**
     * 设置租户ID
     *
     * @param tenantId 租户ID
     */
    public static void setTenantId(String tenantId) {
        if (tenantId != null) {
            MDC.put(LogConstants.TENANT_ID, tenantId);
        }
    }

    /**
     * 获取租户ID
     *
     * @return 租户ID
     */
    public static String getTenantId() {
        return MDC.get(LogConstants.TENANT_ID);
    }

    /**
     * 设置操作类型
     *
     * @param operation 操作类型
     */
    public static void setOperation(String operation) {
        if (operation != null) {
            MDC.put(LogConstants.OPERATION, operation);
        }
    }

    /**
     * 获取操作类型
     *
     * @return 操作类型
     */
    public static String getOperation() {
        return MDC.get(LogConstants.OPERATION);
    }

    /**
     * 设置请求URI
     *
     * @param requestUri 请求URI
     */
    public static void setRequestUri(String requestUri) {
        if (requestUri != null) {
            MDC.put(LogConstants.REQUEST_URI, requestUri);
        }
    }

    /**
     * 获取请求URI
     *
     * @return 请求URI
     */
    public static String getRequestUri() {
        return MDC.get(LogConstants.REQUEST_URI);
    }

    /**
     * 设置请求方法
     *
     * @param method 请求方法
     */
    public static void setRequestMethod(String method) {
        if (method != null) {
            MDC.put(LogConstants.REQUEST_METHOD, method);
        }
    }

    /**
     * 获取请求方法
     *
     * @return 请求方法
     */
    public static String getRequestMethod() {
        return MDC.get(LogConstants.REQUEST_METHOD);
    }

    /**
     * 设置执行耗时
     *
     * @param duration 执行耗时(毫秒)
     */
    public static void setDuration(long duration) {
        MDC.put(LogConstants.DURATION, String.valueOf(duration));
    }

    /**
     * 获取执行耗时
     *
     * @return 执行耗时(毫秒)
     */
    public static String getDuration() {
        return MDC.get(LogConstants.DURATION);
    }

    /**
     * 设置操作是否成功
     *
     * @param success 是否成功
     */
    public static void setSuccess(boolean success) {
        MDC.put(LogConstants.SUCCESS, success ? LogConstants.SUCCESS_TRUE : LogConstants.SUCCESS_FALSE);
    }

    /**
     * 获取操作是否成功
     *
     * @return 是否成功
     */
    public static String getSuccess() {
        return MDC.get(LogConstants.SUCCESS);
    }

    /**
     * 设置请求参数
     *
     * @param params 请求参数
     */
    public static void setRequestParams(String params) {
        if (params != null) {
            MDC.put(LogConstants.REQUEST_PARAMS, params);
        }
    }

    /**
     * 获取请求参数
     *
     * @return 请求参数
     */
    public static String getRequestParams() {
        return MDC.get(LogConstants.REQUEST_PARAMS);
    }

    /**
     * 设置请求IP
     *
     * @param ip 请求IP
     */
    public static void setRequestIp(String ip) {
        if (ip != null) {
            MDC.put(LogConstants.REQUEST_IP, ip);
        }
    }

    /**
     * 获取请求IP
     *
     * @return 请求IP
     */
    public static String getRequestIp() {
        return MDC.get(LogConstants.REQUEST_IP);
    }

    /**
     * 获取User-Agent
     *
     * @return User-Agent
     */
    public static String getUserAgent() {
        return MDC.get(LogConstants.USER_AGENT);
    }

    /**
     * 设置上下文信息
     *
     * @param userName  用户代码
     * @param tenantId  租户ID
     * @param operation 操作类型
     */
    public static void setContext(String userName, String tenantId, String operation) {
        setUserName(userName);
        setTenantId(tenantId);
        setOperation(operation);
    }

    /**
     * 批量设置上下文
     *
     * @param contextMap 上下文Map
     */
    public static void putAll(Map<String, String> contextMap) {
        if (contextMap != null) {
            contextMap.forEach((key, value) -> {
                if (key != null && value != null) {
                    MDC.put(key, value);
                }
            });
        }
    }

    /**
     * 捕获当前线程MDC上下文
     *
     * @return MDC上下文副本，当前无上下文时返回空Map
     */
    public static Map<String, String> captureContext() {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return contextMap == null ? Map.of() : Map.copyOf(contextMap);
    }

    /**
     * 使用指定MDC上下文执行任务，执行后恢复之前上下文
     *
     * @param contextMap MDC上下文
     * @param action     任务
     */
    public static void runWithContext(Map<String, String> contextMap, Runnable action) {
        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        try {
            if (contextMap == null || contextMap.isEmpty()) {
                MDC.clear();
            } else {
                MDC.setContextMap(contextMap);
            }
            action.run();
        } finally {
            if (previousContext == null || previousContext.isEmpty()) {
                MDC.clear();
            } else {
                MDC.setContextMap(previousContext);
            }
        }
    }

    /**
     * 使用指定 MDC 上下文执行 Callable，执行后恢复之前上下文。
     *
     * @param contextMap MDC 上下文
     * @param action     任务
     * @param <T>        返回类型
     * @return 任务结果
     * @throws Exception 任务抛出的异常
     */
    public static <T> T callWithContext(Map<String, String> contextMap, java.util.concurrent.Callable<T> action)
            throws Exception {
        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        try {
            if (contextMap == null || contextMap.isEmpty()) {
                MDC.clear();
            } else {
                MDC.setContextMap(contextMap);
            }
            return action.call();
        } finally {
            if (previousContext == null || previousContext.isEmpty()) {
                MDC.clear();
            } else {
                MDC.setContextMap(previousContext);
            }
        }
    }

    /**
     * 使用指定traceId执行任务，执行后恢复之前上下文
     *
     * @param traceId traceId，为空时自动生成
     * @param action  任务
     */
    public static void runWithTraceId(String traceId, Runnable action) {
        Map<String, String> contextMap = captureContext();
        if (traceId == null || traceId.isBlank()) {
            contextMap = new HashMap<>(contextMap);
            contextMap.put(LogConstants.TRACE_ID, UUID.randomUUID().toString().replace("-", ""));
        } else {
            contextMap = new HashMap<>(contextMap);
            contextMap.put(LogConstants.TRACE_ID, traceId);
        }
        runWithContext(contextMap, action);
    }

    /**
     * 清理所有MDC上下文
     */
    public static void clear() {
        MDC.clear();
    }

    /**
     * 移除指定key
     *
     * @param key MDC key
     */
    public static void remove(String key) {
        if (key != null) {
            MDC.remove(key);
        }
    }

    /**
     * 设置任意key-value到MDC
     *
     * @param key   MDC key
     * @param value MDC value
     */
    public static void put(String key, String value) {
        if (key != null && value != null) {
            MDC.put(key, value);
        }
    }
}
