package com.scaffold.mes.exec.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.AppendOnlyEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * WaferHistory Wafer 事务历史
 * <p>
 * 对应 hist_mes_wafer_history。只追加。只记录片自身发生变化的事件，Lot 级 Track 不逐片写入。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("hist_mes_wafer_history")
public class WaferHistory extends AppendOnlyEntity {

    /** Wafer。 */
    private Long waferId;

    /** 本厂编号。 */
    private String waferCode;

    /** 发生时所属 Lot。 */
    private Long lotId;

    /** Lot 号。 */
    private String lotNo;

    /** 事务类型。 */
    private String txnType;

    /** 步骤序号。 */
    private Integer routeSeqNo;

    /** 站点。 */
    private Long stepId;

    /** 第几次经过，只用于展示。 */
    private Integer passNo;

    /** 变更前 Slot。 */
    private Integer slotNoBefore;

    /** 变更后 Slot。 */
    private Integer slotNoAfter;

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
