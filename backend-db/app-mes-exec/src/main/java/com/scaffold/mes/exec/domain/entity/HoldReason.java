package com.scaffold.mes.exec.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * HoldReason Hold 原因码
 * <p>
 * 对应 mes_hold_reason。Hold 是叠加标记，每条 Lot Hold 引用一个原因码。空的 releaseRoleCode 表示任何有权限者都可以 Release。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_hold_reason")
public class HoldReason extends NonSoftDeletableEntity {

    /** 原因码，租户内唯一。 */
    private String reasonCode;

    /** 原因名称。 */
    private String reasonName;

    /** 类别：QUALITY、EQUIPMENT、PROCESS、ENGINEERING、MATERIAL、OTHER。 */
    private String holdType;

    /** 允许 Release 的角色编码。空表示不限角色。 */
    private String releaseRoleCode;

    /** 备注。 */
    private String remark;

    /** 1 启用，0 停用。 */
    private Integer enabled;
}
