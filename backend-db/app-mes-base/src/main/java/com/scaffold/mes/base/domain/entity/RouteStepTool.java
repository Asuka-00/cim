package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * RouteStepTool Route 步骤工具要求
 * <p>
 * 对应 mes_route_step_tool。有记录时整组替换站点要求。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_route_step_tool")
public class RouteStepTool extends NonSoftDeletableEntity {

    /** Route 步骤。 */
    private Long routeStepId;

    /** 所需工具类型。 */
    private Long toolTypeId;

    /** 限定产品。0 表示通用。 */
    private Long productId;
}
