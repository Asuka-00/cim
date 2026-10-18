package com.scaffold.system.domain.dto.rbac;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * OAuth2 一次性 ticket 换票请求
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-07-17
 */
@Data
public class OAuth2TicketExchangeRequestDTO {

    @NotBlank
    private String ticket;
}
