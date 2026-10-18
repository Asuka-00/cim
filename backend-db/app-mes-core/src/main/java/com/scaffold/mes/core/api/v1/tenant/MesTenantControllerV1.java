package com.scaffold.mes.core.api.v1.tenant;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.core.domain.service.tenant.MesTenantInitializer;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import lombok.RequiredArgsConstructor;

/**
 * MesTenantControllerV1 租户 MES 初始化接口
 * <p>
 * 为指定租户幂等写入 MES 种子数据。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/tenants")
@RequiredArgsConstructor
public class MesTenantControllerV1 {

    private final MesTenantInitializer initializer;

    /**
     * 为指定租户幂等初始化 MES 种子。
     *
     * @param tenantId 租户标识
     * @return 固定为 true
     */
    @PostMapping("/{tenantId}/init")
    @AuditLog(operation = "初始化租户 MES 种子")
    public Result<Boolean> init(@PathVariable String tenantId) {
        initializer.init(tenantId);
        return Result.success(Boolean.TRUE);
    }
}
