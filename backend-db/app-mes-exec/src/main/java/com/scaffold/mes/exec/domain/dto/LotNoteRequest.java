package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * LotNoteRequest 追加备注
 * <p>
 * 类型空则按 NOTE。pinned 为 1 时置顶。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class LotNoteRequest {

    /** 类型。 */
    private String noteType;

    /** 内容。 */
    @NotBlank
    private String content;

    /** 1 表示置顶。 */
    private Integer pinned;
}
