package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * StepRecipe 站点 Recipe 绑定
 * <p>
 * 对应 mes_step_recipe。productId 为 0 表示不限产品。查找时先用限定产品，没有再退回通用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_step_recipe")
public class StepRecipe extends NonSoftDeletableEntity {

    /** 站点。 */
    private Long stepId;

    /** Recipe。 */
    private Long recipeId;

    /** 限定产品。0 表示通用。 */
    private Long productId;

    /** 1 表示该站点、该产品范围内的默认 Recipe。 */
    private Integer isDefault;
}
