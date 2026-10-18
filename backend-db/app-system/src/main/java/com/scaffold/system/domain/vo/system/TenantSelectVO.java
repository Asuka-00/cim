package com.scaffold.system.domain.vo.system;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * TenantSelectVO下拉选择视图对象
 * <p>
 * 负责承载Tenant下拉列表相关展示数据
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class TenantSelectVO {

    /**
     * 主键ID
     */
    private String id;

    /**
     * 租户编码
     */
    private String tenantId;

    /**
     * 租户名称
     */
    private String tenantName;
}
