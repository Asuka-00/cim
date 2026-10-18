package com.scaffold.mes.warehouse.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Receipt 收货单
 * <p>
 * 对应 mes_receipt。本阶段一次确认即完成。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_receipt")
public class Receipt extends NonSoftDeletableEntity {

    /** 收货单号。 */
    private String receiptNo;

    /** CONFIRMED。 */
    private String status;

    /** 收货时间。 */
    private LocalDateTime receivedAt;
}
