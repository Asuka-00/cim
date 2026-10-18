package com.scaffold.mes.exec.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * WorkOrder 工单
 * <p>
 * 对应 mes_work_order。Route 和 BOM 版本在创建时固化，之后升版不影响本工单。进度字段由事件异步重算，创建时保持 0。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_work_order")
public class WorkOrder extends NonSoftDeletableEntity {

    /** 工单号，租户内唯一。 */
    private String woNo;

    /** 产品。 */
    private Long productId;

    /** 下单时的产品版本。 */
    private Long productVersionId;

    /** 下单时固化的 Route 版本。 */
    private Long routeVersionId;

    /** 下单时固化的 BOM 版本。 */
    private Long bomVersionId;

    /** 客户。 */
    private Long customerId;

    /** 计划数量。 */
    private Long planQty;

    /** 起始计数单位。进度都按这个单位算。 */
    private Long qtyUnitId;

    /** 优先级，1 最高，9 最低。 */
    private Integer priority;

    /** 交期。 */
    private LocalDate dueDate;

    /** 状态。 */
    private String status;

    /** 已开批数。异步汇总，创建时为 0。 */
    private Integer lotCount;

    /** 已投入数量。异步汇总。 */
    private Long startedQty;

    /** 已完成数量。异步汇总。 */
    private Long completedQty;

    /** 已报废数量。异步汇总。 */
    private Long scrapQty;

    /** 已终止剩余数量。不计入完成。 */
    private Long terminatedQty;

    /** 下达时间。 */
    private LocalDateTime releasedAt;

    /** 关闭时间。 */
    private LocalDateTime closedAt;

    /** 来源：MANUAL 或 IMPORT。 */
    private String source;

    /** 备注。 */
    private String remark;

    /** 乐观锁版本号。 */
    @Version
    private Integer version;
}
