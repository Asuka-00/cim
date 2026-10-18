package com.scaffold.mes.base.domain.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * RouteStepCollectItem Route 步骤采集规格
 * <p>
 * 对应 mes_route_step_collect_item。这一步只要有记录，就整组替换站点采集规格。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_route_step_collect_item")
public class RouteStepCollectItem extends NonSoftDeletableEntity {

    /** Route 版本。 */
    private Long routeVersionId;

    /** Route 步骤。 */
    private Long routeStepId;

    /** 采集项。 */
    private Long itemId;

    /** 产品。0 表示通用。 */
    private Long productId;

    /** 规格下限。 */
    private BigDecimal lsl;

    /** 规格上限。 */
    private BigDecimal usl;

    /** 1 表示超规格自动 Hold。 */
    private Integer autoHoldOnOos;

    /** 自动 Hold 原因码。 */
    private String holdReasonCode;

    /** 1 表示出站前必须有有效值。 */
    private Integer isRequired;
}
