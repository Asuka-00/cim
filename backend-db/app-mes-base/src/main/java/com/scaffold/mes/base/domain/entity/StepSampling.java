package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * StepSampling 站点抽检绑定
 * <p>
 * 对应 mes_step_sampling。同一站点与产品只能绑一条规则。productId 为 0 表示不限产品。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_step_sampling")
public class StepSampling extends NonSoftDeletableEntity {

    /** 量测站点。 */
    private Long stepId;

    /** 限定产品。0 表示通用。 */
    private Long productId;

    /** 抽检规则。 */
    private Long ruleId;
}
