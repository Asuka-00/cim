package com.scaffold.system.domain.query.system;

import com.scaffold.share.enums.StateEnum;

import lombok.Data;

/**
 * TenantQuery类
 * <p>
 * 负责处理TenantQuery相关核心业务能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
public class TenantQuery {

    /**
     * 租户编码
     */
    private String tenantId;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * 租户状态
     */
    private StateEnum enabled;
}
