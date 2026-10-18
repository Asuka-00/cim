package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * RouteStepSampling Route 步骤抽检覆盖
 * <p>
 * 对应 mes_route_step_sampling。这一步只要有记录，就整组替换站点抽检绑定。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_route_step_sampling")
public class RouteStepSampling extends NonSoftDeletableEntity {

    /** Route 版本。 */
    private Long routeVersionId;

    /** Route 步骤。 */
    private Long routeStepId;

    /** 限定产品。0 表示通用。 */
    private Long productId;

    /** 抽检规则。 */
    private Long ruleId;
}
