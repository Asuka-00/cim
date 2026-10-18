package com.scaffold.system.domain.vo.system;

import com.scaffold.share.enums.StateEnum;
import com.scaffold.share.vo.BaseVO;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * TenantVO视图对象
 * <p>
 * 负责承载TenantVO相关展示与返回数据
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class TenantVO extends BaseVO {

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
