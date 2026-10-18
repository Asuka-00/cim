package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * RouteTimeLink Route 时间链
 * <p>
 * 对应 mes_route_time_link。约束从起点步骤 Track-Out 到终点步骤 Track-In 的最长间隔。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_route_time_link")
public class RouteTimeLink extends NonSoftDeletableEntity {

    /** 所属 Route 版本。 */
    private Long routeVersionId;

    /** 起点步骤序号。 */
    private Integer fromSeqNo;

    /** 终点步骤序号。 */
    private Integer toSeqNo;

    /** 最长间隔，分钟。 */
    private Integer maxMinutes;

    /** 预警阈值，分钟。 */
    private Integer warnMinutes;

    /** 超时动作：HOLD 或 WARN。 */
    private String action;

    /** 自动 Hold 的原因码。 */
    private String reasonCode;
}
