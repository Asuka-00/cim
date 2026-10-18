package com.scaffold.mes.warehouse.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * IncomingInspection 来料检验
 * <p>
 * 对应 mes_incoming_inspection。合格和特采后物料批可用，不合格进隔离。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_incoming_inspection")
public class IncomingInspection extends NonSoftDeletableEntity {

    /** 物料批。 */
    private Long materialLotId;

    /** PASS、FAIL 或 CONCESSION。 */
    private String result;

    /** 检验人。 */
    private String inspector;

    /** 检验时间。 */
    private LocalDateTime inspectedAt;

    /** 备注。特采时必填。 */
    private String remark;
}
