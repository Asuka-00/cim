package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * RouteStepRecipe 步骤 Recipe 覆盖
 * <p>
 * 对应 mes_route_step_recipe。只要本步骤有匹配记录，就整体替换站点级绑定，不与站点级合并。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_route_step_recipe")
public class RouteStepRecipe extends NonSoftDeletableEntity {

    /** 所属 Route 版本。 */
    private Long routeVersionId;

    /** Route 步骤。 */
    private Long routeStepId;

    /** Recipe。 */
    private Long recipeId;

    /** 限定产品。0 表示通用。 */
    private Long productId;

    /** 1 表示该步骤、该产品范围内的默认 Recipe。 */
    private Integer isDefault;
}
