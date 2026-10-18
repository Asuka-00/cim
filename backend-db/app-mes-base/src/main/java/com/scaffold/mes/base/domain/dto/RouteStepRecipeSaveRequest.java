package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * RouteStepRecipeSaveRequest 步骤 Recipe 覆盖保存请求
 * <p>
 * 新增与修改共用。未传产品时按通用。版本号从步骤上带出，不由调用方指定。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class RouteStepRecipeSaveRequest {

    /** Route 步骤。 */
    @NotNull
    private Long routeStepId;

    /** Recipe。 */
    @NotNull
    private Long recipeId;

    /** 限定产品。空则按通用。 */
    private Long productId;

    /** 1 表示默认。空则按否。 */
    private Integer isDefault;
}
