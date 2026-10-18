package com.scaffold.mes.exec.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * BatchLot 批次成员
 * <p>
 * 对应 mes_batch_lot。同一批次和 Lot 只有一行。移出后改为 REMOVED，再加入时改回 IN。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_batch_lot")
public class BatchLot extends NonSoftDeletableEntity {

    /** 批次。 */
    private Long batchId;

    /** Lot。 */
    private Long lotId;

    /** 组内序号。 */
    private Integer seq;

    /** 加入时数量。 */
    private Long qty;

    /** IN 或 REMOVED。 */
    private String status;

    /** 加入时间。 */
    private LocalDateTime addedAt;

    /** 移出时间。 */
    private LocalDateTime removedAt;

    /** 移出原因。 */
    private String removeReason;
}
