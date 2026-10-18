package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Route 工艺路线主数据
 * <p>
 * 对应 mes_route。步骤和版本在 mes_route_version、mes_route_step，不写在这张主表上。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_route")
public class Route extends NonSoftDeletableEntity {

    /** Route 编码，租户内唯一。 */
    private String routeCode;

    /** Route 名称。 */
    private String routeName;

    /** 说明。 */
    private String description;

    /** 1 启用，0 停用。 */
    private Integer enabled;
}
