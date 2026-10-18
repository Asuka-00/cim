package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Material 物料
 * <p>
 * 对应 mes_material。基础计量单位用 mes_uom，和 Lot 的计数单位分开。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_material")
public class Material extends NonSoftDeletableEntity {

    /** 物料编码，租户内唯一。 */
    private String materialCode;

    /** 物料名称。 */
    private String materialName;

    /** 类型：WAFER、RAW、AUX、CHEMICAL、GAS、PACKAGE、OTHER。 */
    private String materialType;

    /** 规格型号。 */
    private String spec;

    /** 基础计量单位。 */
    private Long baseUomId;

    /** 1 表示关键料，缺料或过期时 Interlock 阻断。 */
    private Integer isCritical;

    /** 保质期天数。空表示不限。 */
    private Integer shelfLifeDays;

    /** 1 表示来料要检验。 */
    private Integer needIncomingInspect;

    /** 备注。 */
    private String remark;

    /** 1 启用，0 停用。 */
    private Integer enabled;
}
