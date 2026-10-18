package com.scaffold.mes.base.domain.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * EquipmentCapabilitySaveRequest 设备能力保存请求
 * <p>
 * 新增与修改共用。未传 Recipe 或产品时按通用。未传状态时按 VALID。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class EquipmentCapabilitySaveRequest {

    /** 设备。 */
    @NotNull
    private Long equipmentId;

    /** 站点。 */
    @NotNull
    private Long stepId;

    /** 限定 Recipe。空则按通用。 */
    private Long recipeId;

    /** 限定产品。空则按通用。 */
    private Long productId;

    /** 状态。空则按 VALID。 */
    private String status;

    /** 资格开始。 */
    private LocalDateTime validFrom;

    /** 资格结束。 */
    private LocalDateTime validTo;

    /** 认证人。 */
    private String qualifiedBy;

    /** 备注。 */
    private String remark;
}
