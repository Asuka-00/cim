package com.scaffold.mes.base.domain.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * RouteStep Route 步骤
 * <p>
 * 对应 mes_route_step。序号在版本内唯一。同一站点可以出现多次，用别名区分重入。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_route_step")
public class RouteStep extends NonSoftDeletableEntity {

    /** 所属 Route 版本。 */
    private Long routeVersionId;

    /** 步骤序号。 */
    private Integer seqNo;

    /** 站点。允许在同一版本内重复。 */
    private Long stepId;

    /** 重入时的别名。 */
    private String stepAlias;

    /** 1 表示必经。 */
    private Integer isRequired;

    /** 步骤指定设备组。空表示用站点级绑定。 */
    private Long equipmentGroupId;

    /** 进入本步骤时的计数单位。空表示沿用上一步。 */
    private Long countUnitId;

    /** 1 表示单位转换站点。 */
    private Integer isUnitConvert;

    /** 转换后的计数单位。 */
    private Long convertToUnitId;

    /** 标准加工时长，分钟。 */
    private BigDecimal stdProcessMin;

    /** 进站到 Track-In 的最长等待，分钟。空表示不限。 */
    private Integer queueTimeMaxMin;

    /** Queue Time 预警阈值，分钟。 */
    private Integer queueTimeWarnMin;

    /** 超时动作：HOLD 或 WARN。 */
    private String queueTimeAction;

    /** 1 表示抽检站点。没抽中的 Lot 出站时跳过。 */
    private Integer isSamplingStep;
}
