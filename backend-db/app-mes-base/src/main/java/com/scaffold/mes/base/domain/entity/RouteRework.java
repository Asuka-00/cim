package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * RouteRework 返工分支
 * <p>
 * 对应 mes_route_rework。规定从哪些主流程步骤可以进入，以及最多进入几次。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_route_rework")
public class RouteRework extends NonSoftDeletableEntity {

    /** 所属 Route 版本。 */
    private Long routeVersionId;

    /** 编码。 */
    private String reworkCode;

    /** 名称。 */
    private String reworkName;

    /** 允许进入的主流程序号起。 */
    private Integer entryFromSeq;

    /** 允许进入的主流程序号止。 */
    private Integer entryToSeq;

    /** 走完后回到的主流程序号。 */
    private Integer returnSeqNo;

    /** 同一根 Lot 的最大次数。 */
    private Integer maxCount;

    /** 1 启用，0 停用。 */
    private Integer enabled;
}
