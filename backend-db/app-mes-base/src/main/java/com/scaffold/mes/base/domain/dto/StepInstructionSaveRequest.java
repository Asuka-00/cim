package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * StepInstructionSaveRequest 保存站点作业指导
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class StepInstructionSaveRequest {

    /** 站点。 */
    @NotNull
    private Long stepId;

    /** 限定产品。空表示通用。 */
    private Long productId;

    /** 标题。 */
    @NotBlank
    private String title;

    /** 正文。 */
    @NotBlank
    private String content;

    /** 已登记的文件。空表示不挂附件。 */
    private Long fileId;
}
