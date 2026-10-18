package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Recipe Recipe 主数据
 * <p>
 * 对应 mes_recipe。只保存编码和适用设备类型，设备侧程序体不入库。版本在 mes_recipe_version。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_recipe")
public class Recipe extends NonSoftDeletableEntity {

    /** Recipe 编码，租户内唯一。 */
    private String recipeCode;

    /** Recipe 名称。 */
    private String recipeName;

    /** 适用设备类型。空表示不限。 */
    private String equipmentType;

    /** 说明。 */
    private String description;

    /** 1 启用，0 停用。 */
    private Integer enabled;
}
