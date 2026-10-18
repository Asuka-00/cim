package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * CountUnit 计数单位
 * <p>
 * 对应 mes_count_unit。表示 Lot 的件数，如 Wafer、Die，与物料计量单位分开。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_count_unit")
public class CountUnit extends NonSoftDeletableEntity {

    /** 单位编码，租户内唯一。 */
    private String unitCode;

    /** 单位名称。 */
    private String unitName;

    /** 类别：WAFER、DIE、STRIP、UNIT、CARRIER、OTHER。 */
    private String unitType;

    /** 1 表示最小可计数单位。 */
    private Integer isBase;

    /** 排序号。 */
    private Integer sortNo;

    /** 备注。 */
    private String remark;

    /** 1 启用，0 停用。 */
    private Integer enabled;
}
