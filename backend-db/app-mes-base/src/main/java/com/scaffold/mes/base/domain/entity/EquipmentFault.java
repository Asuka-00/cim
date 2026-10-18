package com.scaffold.mes.base.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * EquipmentFault 设备异常
 * <p>
 * 对应 mes_equipment_fault。登记后是 OPEN，关闭时写下处理结果。不自动改设备状态。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_equipment_fault")
public class EquipmentFault extends NonSoftDeletableEntity {

    /** 异常单号。 */
    private String faultNo;

    /** 设备。 */
    private Long equipmentId;

    /** MINOR、MAJOR 或 CRITICAL。 */
    private String severity;

    /** 描述。 */
    private String description;

    /** 发生时间。 */
    private LocalDateTime occurredAt;

    /** 登记人。 */
    private String reportedBy;

    /** OPEN 或 CLOSED。 */
    private String status;

    /** 处理结果。 */
    private String solution;

    /** 影响 Lot 数。 */
    private Integer affectedLotCount;
}
