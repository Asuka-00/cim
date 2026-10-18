package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * EsignRequest 电子签名
 * <p>
 * 再次确认时签名人是当前用户。第二人签名时填写另一个用户。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class EsignRequest {

    /** 动作编码。 */
    @NotBlank
    private String actionCode;

    /** 业务对象类型。 */
    @NotBlank
    private String bizType;

    /** 业务对象。 */
    @NotNull
    private Long bizId;

    /** 第二签名人。再次确认时可空。 */
    private String signer;
}
