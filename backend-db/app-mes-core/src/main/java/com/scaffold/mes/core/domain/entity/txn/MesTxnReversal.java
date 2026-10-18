package com.scaffold.mes.core.domain.entity.txn;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.AppendOnlyEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * MesTxnReversal 事务撤销登记
 * <p>
 * 对应 hist_mes_txn_reversal。同一原事务只能登记一次。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("hist_mes_txn_reversal")
public class MesTxnReversal extends AppendOnlyEntity {

    /** 被撤销的原事务 */
    private Long origTxnId;

    /** 执行撤销的纠错事务 */
    private Long reversalTxnId;

    /** 主对象类型 */
    private String objectType;

    /** 主对象ID */
    private Long objectId;

    /** 操作人 */
    private String operator;

    /** 原因码 */
    private String reasonCode;

    /** 撤销时间 */
    private LocalDateTime reversedAt;

}
