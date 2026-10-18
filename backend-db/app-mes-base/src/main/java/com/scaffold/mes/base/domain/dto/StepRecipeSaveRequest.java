package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * StepRecipeSaveRequest 站点 Recipe 绑定保存请求
 * <p>
 * 新增与修改共用。未传产品时按通用。设为默认时，同站点同产品下其他默认会被清掉。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class StepRecipeSaveRequest {

    /** 站点。 */
    @NotNull
    private Long stepId;

    /** Recipe。 */
    @NotNull
    private Long recipeId;

    /** 限定产品。空则按通用。 */
    private Long productId;

    /** 1 表示默认 Recipe。空则按否。 */
    private Integer isDefault;
}
