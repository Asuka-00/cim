package com.scaffold.mes.core.domain.entity.uda;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * UdaDef 自定义属性定义
 * <p>
 * 对应 mes_uda_def。同一对象类型下属性编码不能重复。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_uda_def")
public class UdaDef extends NonSoftDeletableEntity {

    /** LOT、WAFER、PRODUCT、EQUIPMENT、MATERIAL 或 WORK_ORDER。 */
    private String objectType;

    /** 属性编码。 */
    private String attrCode;

    /** 属性名称。 */
    private String attrName;

    /** TEXT、NUMBER、DATE、BOOL 或 ENUM。 */
    private String dataType;

    /** 枚举选项，JSON 数组。 */
    private String optionsJson;

    /** 1 表示必填。 */
    private Integer required;

    /** 1 表示启用。 */
    private Integer enabled;
}
