package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Area 区域
 * <p>
 * 对应 mes_area。parentId 为空表示顶层区域，站点、设备组和设备可以挂在区域下。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_area")
public class Area extends NonSoftDeletableEntity {

    /** 区域编码，租户内唯一。 */
    private String areaCode;

    /** 区域名称。 */
    private String areaName;

    /** 上级区域，空表示顶层。 */
    private Long parentId;

    /** 备注。 */
    private String remark;

    /** 1 启用，0 停用。 */
    private Integer enabled;
}
