package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * StepInstruction 站点作业指导
 * <p>
 * 对应 mes_step_instruction。同一站点与产品只有一条。productId 为 0 表示不限产品。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_step_instruction")
public class StepInstruction extends NonSoftDeletableEntity {

    /** 站点。 */
    private Long stepId;

    /** 限定产品。0 表示通用。 */
    private Long productId;

    /** 标题。 */
    private String title;

    /** 正文。 */
    private String content;

    /** 附件文件。空表示没有附件。 */
    private Long fileId;

    /** 1 表示启用。 */
    private Integer enabled;
}
