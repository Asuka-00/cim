package com.scaffold.system.infra.security.rbac;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;

import com.scaffold.system.domain.dto.rbac.OAuth2ProviderConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * 将系统配置中的厂商 JSON 转为 Spring {@link ClientRegistration}
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-07-17
 */
@Component
@Slf4j
public class OAuth2ClientRegistrationFactory {

    private static final String DEFAULT_REDIRECT_URI = "{baseUrl}/login/oauth2/code/{registrationId}";

    /**
     * 构建 ClientRegistration；失败返回 null（由调用方跳过该厂商）
     */
    public ClientRegistration build(OAuth2ProviderConfig config) {
        if (config == null || !hasText(config.getRegistrationId()) || !hasText(config.getClientId())) {
            return null;
        }
        try {
            if (hasText(config.getIssuerUri())) {
                return buildFromIssuer(config);
            }
            return buildManual(config);
        } catch (Exception ex) {
            log.warn("Build ClientRegistration failed: registrationId={}, error={}",
                    config.getRegistrationId(), ex.getMessage());
            return null;
        }
    }

    private ClientRegistration buildFromIssuer(OAuth2ProviderConfig config) {
        ClientRegistration.Builder builder = ClientRegistrations
                .fromIssuerLocation(config.getIssuerUri().trim())
                .registrationId(config.getRegistrationId().trim())
                .clientId(config.getClientId().trim());
        applyCommon(builder, config);
        return builder.build();
    }

    private ClientRegistration buildManual(OAuth2ProviderConfig config) {
        if (!hasText(config.getAuthorizationUri()) || !hasText(config.getTokenUri())) {
            throw new IllegalArgumentException(
                    "issuerUri 为空时必须提供 authorizationUri 与 tokenUri: " + config.getRegistrationId());
        }
        ClientRegistration.Builder builder = ClientRegistration
                .withRegistrationId(config.getRegistrationId().trim())
                .clientId(config.getClientId().trim())
                .authorizationGrantType(resolveGrantType(config.getAuthorizationGrantType()))
                .authorizationUri(config.getAuthorizationUri().trim())
                .tokenUri(config.getTokenUri().trim());
        if (hasText(config.getUserInfoUri())) {
            builder.userInfoUri(config.getUserInfoUri().trim());
        }
        if (hasText(config.getJwkSetUri())) {
            builder.jwkSetUri(config.getJwkSetUri().trim());
        }
        applyCommon(builder, config);
        return builder.build();
    }

    private void applyCommon(ClientRegistration.Builder builder, OAuth2ProviderConfig config) {
        if (hasText(config.getClientSecret())) {
            builder.clientSecret(config.getClientSecret());
        }
        builder.clientAuthenticationMethod(resolveAuthMethod(config.getClientAuthenticationMethod()));
        builder.clientName(hasText(config.getClientName()) ? config.getClientName() : config.getRegistrationId());
        builder.redirectUri(hasText(config.getRedirectUri()) ? config.getRedirectUri() : DEFAULT_REDIRECT_URI);
        Set<String> scopes = parseScopes(config.getScope());
        if (!scopes.isEmpty()) {
            builder.scope(scopes);
        }
        if (hasText(config.getUserNameAttribute())) {
            builder.userNameAttributeName(config.getUserNameAttribute().trim());
        }
    }

    private static Set<String> parseScopes(String scope) {
        if (!hasText(scope)) {
            return Set.of();
        }
        return Arrays.stream(scope.split("[,\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static ClientAuthenticationMethod resolveAuthMethod(String method) {
        if (!hasText(method)) {
            return ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
        }
        String m = method.trim().toLowerCase();
        return switch (m) {
            case "client_secret_post", "post" -> ClientAuthenticationMethod.CLIENT_SECRET_POST;
            case "none" -> ClientAuthenticationMethod.NONE;
            case "client_secret_jwt" -> ClientAuthenticationMethod.CLIENT_SECRET_JWT;
            case "private_key_jwt" -> ClientAuthenticationMethod.PRIVATE_KEY_JWT;
            default -> ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
        };
    }

    private static AuthorizationGrantType resolveGrantType(String grantType) {
        if (!hasText(grantType)) {
            return AuthorizationGrantType.AUTHORIZATION_CODE;
        }
        String g = grantType.trim().toLowerCase();
        if ("client_credentials".equals(g)) {
            return AuthorizationGrantType.CLIENT_CREDENTIALS;
        }
        if ("refresh_token".equals(g)) {
            return AuthorizationGrantType.REFRESH_TOKEN;
        }
        return AuthorizationGrantType.AUTHORIZATION_CODE;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
