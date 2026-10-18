package com.scaffold.mes.exec.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * DispatchRule 作业清单排序规则
 * <p>
 * 对应 mes_dispatch_rule。sort_keys 按逗号分隔，每一段是键和方向，例如 IS_HOT:DESC,PRIORITY:ASC。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_dispatch_rule")
public class DispatchRule extends NonSoftDeletableEntity {

    /** 规则编码。 */
    private String ruleCode;

    /** 规则名称。 */
    private String ruleName;

    /** GLOBAL、STEP 或 AREA。 */
    private String scopeType;

    /** 范围对象。GLOBAL 时为 0。 */
    private Long scopeId;

    /** 排序键。 */
    private String sortKeys;

    /** 1 表示没有指定规则时使用。 */
    private Integer isDefault;

    /** 1 启用，0 停用。 */
    private Integer enabled;
}
