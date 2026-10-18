package com.scaffold.system.domain.service.rbac;

import java.util.Collections;
import java.util.List;

import com.scaffold.system.domain.dto.rbac.OAuth2ProviderConfig;

import lombok.Builder;
import lombok.Value;

/**
 * 从系统配置表解析出的 OAuth2 运行时设置
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-07-17
 */
@Value
@Builder
public class OAuth2Settings {

    boolean enabled;

    String userNameClaim;

    String frontendSuccessUrl;

    String frontendFailureUrl;

    long ticketTtlSeconds;

    @Builder.Default
    List<OAuth2ProviderConfig> providers = Collections.emptyList();

    public static OAuth2Settings defaults() {
        return OAuth2Settings.builder()
                .enabled(false)
                .userNameClaim("preferred_username")
                .frontendSuccessUrl("http://localhost:2900/#/login/oauth2/callback")
                .frontendFailureUrl("http://localhost:2900/#/login")
                .ticketTtlSeconds(60)
                .providers(Collections.emptyList())
                .build();
    }
}
