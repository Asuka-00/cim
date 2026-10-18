package com.scaffold.mes.warehouse.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * MaterialLot 物料批
 * <p>
 * 对应 mes_material_lot。需要检验时先停在 PENDING_QC，否则收货后就是 AVAILABLE。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_material_lot")
public class MaterialLot extends NonSoftDeletableEntity {

    /** 物料批号。 */
    private String lotCode;

    /** 物料。 */
    private Long materialId;

    /** 收货数量。 */
    private BigDecimal receivedQty;

    /** 计量单位。 */
    private Long uomId;

    /** 到期日期。早于今天且仍待用时改为 EXPIRED。 */
    private java.time.LocalDate expireDate;

    /** PENDING_QC、AVAILABLE 或 QUARANTINE。 */
    private String status;

    /** PASS、FAIL 或 CONCESSION。 */
    private String qcResult;

    /** 收货时间。 */
    private LocalDateTime receivedAt;

    /** 乐观锁版本号。 */
    @Version
    private Integer version;
}
