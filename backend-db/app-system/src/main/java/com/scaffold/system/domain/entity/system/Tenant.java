package com.scaffold.system.domain.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;
import com.scaffold.share.enums.StateEnum;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Tenant 租户实体
 * <p>
 * 记录租户基本信息，包括编码、名称、联系方式和过期时间。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Getter
@Setter
@FieldNameConstants
@TableName("sys_tenant")
public class Tenant extends NonSoftDeletableEntity {

    /**
     * 租户名称（公司/组织名称）
     */
    private String tenantName;

    /**
     * 租户状态(0:禁用 1:启用)
     */
    private StateEnum enabled;
}
