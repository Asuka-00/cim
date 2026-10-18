package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * StepEquipmentGroup 站点设备组绑定
 * <p>
 * 对应 mes_step_equipment_group。同一站点可绑多个设备组，priority 越小越优先。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_step_equipment_group")
public class StepEquipmentGroup extends NonSoftDeletableEntity {

    /** 站点。 */
    private Long stepId;

    /** 设备组。 */
    private Long groupId;

    /** 优先级，数小优先。 */
    private Integer priority;
}
