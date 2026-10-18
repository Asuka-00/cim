package com.scaffold.mes.exec.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.AppendOnlyEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * LotHistory Lot 事务历史
 * <p>
 * 对应 hist_mes_lot_history。只追加。before_json 只留给可撤销的事务，用来恢复变更前的 Lot。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("hist_mes_lot_history")
public class LotHistory extends AppendOnlyEntity {

    /** Lot。 */
    private Long lotId;

    /** Lot 号。 */
    private String lotNo;

    /** 事务。 */
    private Long txnId;

    /** 事务类型。 */
    private String txnType;

    /** Route 版本。 */
    private Long routeVersionId;

    /** 步骤序号。 */
    private Integer routeSeqNo;

    /** 站点。 */
    private Long stepId;

    /** 第几次经过，只用于展示。 */
    private Integer passNo;

    /** 返工分支。进入返工时写入，用来按根 Lot 计次。 */
    private Long reworkId;

    /** 所属批次。批次结束后仍可从历史反查。 */
    private Long batchId;

    /** 变更前数量。 */
    private Long qtyBefore;

    /** 变更后数量。 */
    private Long qtyAfter;

    /** 变更前计数单位。 */
    private Long unitBeforeId;

    /** 变更后计数单位。 */
    private Long unitAfterId;

    /** 变更前状态。 */
    private String statusBefore;

    /** 变更后状态。 */
    private String statusAfter;

    /** 原因码。 */
    private String reasonCode;

    /** 操作人。 */
    private String operator;

    /** 发生时间。 */
    private LocalDateTime occurredAt;

    /** 备注。 */
    private String remark;

    /** 变更前快照。不可撤销的事务保持为空。 */
    private String beforeJson;
}
