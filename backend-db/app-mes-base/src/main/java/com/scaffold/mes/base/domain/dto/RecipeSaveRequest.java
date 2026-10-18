package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * RecipeSaveRequest Recipe 保存请求
 * <p>
 * 新增与修改共用。适用设备类型有值时必须是设备类型枚举。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class RecipeSaveRequest {

    /** Recipe 编码，租户内唯一。 */
    @NotBlank
    private String recipeCode;

    /** Recipe 名称。 */
    @NotBlank
    private String recipeName;

    /** 适用设备类型。空表示不限。 */
    private String equipmentType;

    /** 说明。 */
    private String description;

    /** 1 启用，0 停用。空则按启用处理。 */
    private Integer enabled;
}
