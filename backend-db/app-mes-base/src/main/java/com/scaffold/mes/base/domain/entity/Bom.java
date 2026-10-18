package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Bom 物料清单主数据
 * <p>
 * 对应 mes_bom。版本和行在 mes_bom_version、mes_bom_item。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_bom")
public class Bom extends NonSoftDeletableEntity {

    /** BOM 编码，租户内唯一。 */
    private String bomCode;

    /** BOM 名称。 */
    private String bomName;

    /** 说明。 */
    private String description;

    /** 1 启用，0 停用。 */
    private Integer enabled;
}
