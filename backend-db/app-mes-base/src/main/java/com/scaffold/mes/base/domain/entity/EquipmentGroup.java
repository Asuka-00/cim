package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * EquipmentGroup 设备组
 * <p>
 * 对应 mes_equipment_group。同一类设备归到一组，设备通过 group_id 引用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_equipment_group")
public class EquipmentGroup extends NonSoftDeletableEntity {

    /** 设备组编码，租户内唯一。 */
    private String groupCode;

    /** 设备组名称。 */
    private String groupName;

    /** 所属区域，可空。 */
    private Long areaId;

    /** 备注。 */
    private String remark;

    /** 1 启用，0 停用。 */
    private Integer enabled;
}
