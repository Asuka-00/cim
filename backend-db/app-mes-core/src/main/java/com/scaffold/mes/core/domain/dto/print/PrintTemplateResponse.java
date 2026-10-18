package com.scaffold.mes.core.domain.dto.print;

/**
 * PrintTemplateResponse 当前默认打印模板
 *
 * @param tplCode 模板编码
 * @param tplName 模板名称
 * @param content 模板内容
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record PrintTemplateResponse(String tplCode, String tplName, String content) {
}
