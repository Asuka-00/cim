package com.scaffold.mes.base.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * EquipmentCapability 设备能力
 * <p>
 * 对应 mes_equipment_capability。recipeId 或 productId 为 0 表示不限。SUSPENDED 即 Inhibit。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_equipment_capability")
public class EquipmentCapability extends NonSoftDeletableEntity {

    /** 设备。 */
    private Long equipmentId;

    /** 可加工站点。 */
    private Long stepId;

    /** 限定 Recipe。0 表示该站点全部 Recipe。 */
    private Long recipeId;

    /** 限定产品。0 表示全部产品。 */
    private Long productId;

    /** 状态：VALID、SUSPENDED、EXPIRED、REVOKED。 */
    private String status;

    /** 资格开始。空表示不限。 */
    private LocalDateTime validFrom;

    /** 资格结束。空表示不限。 */
    private LocalDateTime validTo;

    /** 认证人。 */
    private String qualifiedBy;

    /** 备注。 */
    private String remark;
}
