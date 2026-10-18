package com.scaffold.mes.core.domain.entity.print;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * PrintTemplate 打印模板
 * <p>
 * 对应 mes_print_template。模板编码不能重复。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_print_template")
public class PrintTemplate extends NonSoftDeletableEntity {

    /** 模板编码。 */
    private String tplCode;

    /** 模板名称。 */
    private String tplName;

    /** LOT_TRAVELER、CARRIER_LABEL、WAFER_LABEL、STRIP_LABEL、REEL_LABEL 或 MATERIAL_LABEL。 */
    private String tplType;

    /** HTML 和变量占位。 */
    private String content;

    /** 1 表示该类型的默认模板。 */
    private Integer isDefault;

    /** 1 表示启用。 */
    private Integer enabled;
}
