package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Step 站点
 * <p>
 * 对应 mes_step。目检、HOLD、SHIP、仓储类站点默认不选设备和 Recipe。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_step")
public class Step extends NonSoftDeletableEntity {

    /** 站点编码，租户内唯一。 */
    private String stepCode;

    /** 站点名称。 */
    private String stepName;

    /** 类型：PROCESS、MEASURE、INSPECT、HOLD、SHIP、CONVERT、PACK、STORE。 */
    private String stepType;

    /** 所属区域，可空。 */
    private Long areaId;

    /** 所属站点组，可空。 */
    private Long stepGroupId;

    /** 1 表示 Track 时要选设备。 */
    private Integer needEquipment;

    /** 1 表示 Track 时要选 Recipe。 */
    private Integer needRecipe;

    /** 工艺说明。 */
    private String description;

    /** 1 启用，0 停用。 */
    private Integer enabled;
}
