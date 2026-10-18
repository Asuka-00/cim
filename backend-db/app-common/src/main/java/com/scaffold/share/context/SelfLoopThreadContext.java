package com.scaffold.share.context;

import java.util.Map;
import java.util.UUID;

import com.scaffold.share.logging.MdcUtils;

/**
 * SelfLoopThreadContext 自循环子线程上下文生命周期包装器。
 * <p>
 * 统一承载「租户级 tick 子线程」与「policy 域子线程」的线程上下文重建与清理职责：
 * 子线程不继承父线程 ThreadLocal，需独立设置租户/用户/MDC；执行完毕后清理，避免线程复用时串租户。
 * <p>
 * 通过 try-with-resources 使用：
 * <pre>
 * try (SelfLoopThreadContext ignored = SelfLoopThreadContext.open(tenantId, traceId, Map.of())) {
 *     // 子线程业务逻辑
 * }
 * </pre>
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-07-02
 */
public final class SelfLoopThreadContext implements AutoCloseable {

    private SelfLoopThreadContext() {
    }

    /**
     * 为子线程重建上下文：设置 Tenant / User / MDC，并写入可选的附加 MDC kv。
     * <p>
     * traceId 为 null/空时自动生成（对应原 SelfLoopExecutionContext.openTenant 的兜底行为），
     * 非空时沿用调用方透传的 traceId（对应原 PolicyExecutionContext 沿用父 tick trace 的行为）。
     *
     * @param tenantId 租户 ID
     * @param traceId  traceId，null/空时自动生成
     * @param extraMdc 附加 MDC kv（如 scope/policyId），可为空 Map
     * @return 已打开的上下文，try-with-resources 关闭时清理
     */
    public static SelfLoopThreadContext open(String tenantId, String traceId, Map<String, String> extraMdc) {
        String resolvedTraceId = (traceId == null || traceId.isBlank())
                ? UUID.randomUUID().toString().replace("-", "")
                : traceId;
        TenantContextHolder.setTenantId(tenantId);
        UserContextHolder.setUserName(CurrentSessionProvider.SYSTEM_USER);
        MdcUtils.setTraceId(resolvedTraceId);
        MdcUtils.setTenantId(tenantId);
        MdcUtils.setUserName(CurrentSessionProvider.SYSTEM_USER);
        if (extraMdc != null && !extraMdc.isEmpty()) {
            MdcUtils.putAll(extraMdc);
        }
        return new SelfLoopThreadContext();
    }

    @Override
    public void close() {
        TenantContextHolder.clear();
        UserContextHolder.clear();
        MdcUtils.clear();
    }
}
