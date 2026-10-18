package com.scaffold.mes.exec.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.AppendOnlyEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * DieBinLog Die Bin 变动流水
 * <p>
 * 对应 hist_mes_die_bin_log。只追加。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("hist_mes_die_bin_log")
public class DieBinLog extends AppendOnlyEntity {

    /** Lot。 */
    private Long lotId;

    /** Wafer。0 表示汇总行。 */
    private Long waferId;

    /** Bin 编码。 */
    private String binCode;

    /** 变动量，可以为负。 */
    private Long delta;

    /** 变动后数量。 */
    private Long qtyAfter;

    /** 原因。 */
    private String reason;

    /** 事务。可空。 */
    private Long txnId;

    /** 操作人。 */
    private String operator;

    /** 发生时间。 */
    private LocalDateTime occurredAt;
}
