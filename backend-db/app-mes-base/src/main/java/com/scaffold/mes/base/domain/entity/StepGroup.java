package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * StepGroup 站点组
 * <p>
 * 对应 mes_step_group。把站点归到工艺模块，如 PHOTO、ETCH。站点通过 step_group_id 引用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_step_group")
public class StepGroup extends NonSoftDeletableEntity {

    /** 站点组编码，租户内唯一。 */
    private String groupCode;

    /** 站点组名称。 */
    private String groupName;

    /** 备注。 */
    private String remark;

    /** 1 启用，0 停用。 */
    private Integer enabled;
}
