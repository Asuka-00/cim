package com.scaffold.mes.base.domain.dto;

/**
 * StepInstructionResponse 当前站点要展示的作业指导
 *
 * @param title 标题
 * @param content 正文
 * @param fileId 附件。空表示没有附件
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record StepInstructionResponse(String title, String content, Long fileId) {
}
