package com.scaffold.mes.base.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * EquipmentStatusHist 设备状态历史
 * <p>
 * 对应 hist_mes_equipment_status。插入后只回填 endAt 和 durationSec，其余列保持原值。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("hist_mes_equipment_status")
public class EquipmentStatusHist extends NonSoftDeletableEntity {

    /** 设备。 */
    private Long equipmentId;

    /** Port 或 Chamber。0 表示设备级。 */
    private Long portId;

    /** 变更前 E10。第一段为空。 */
    private String fromE10State;

    /** 本段所处的 E10。 */
    private String toE10State;

    /** 细分状态码。 */
    private String statusCode;

    /** 原因码。 */
    private String reasonCode;

    /** 本段开始时间。 */
    private LocalDateTime startAt;

    /** 本段结束时间。空表示进行中。 */
    private LocalDateTime endAt;

    /** 本段秒数。结束时回填。 */
    private Long durationSec;

    /** 来源：MANUAL、TRACK、SYSTEM。 */
    private String source;

    /** 关联 Lot。手工切换为空。 */
    private Long lotId;

    /** 产生本段的事务。手工切换为空。 */
    private Long txnId;

    /** 操作人。 */
    private String operator;

    /** 备注。 */
    private String remark;
}
