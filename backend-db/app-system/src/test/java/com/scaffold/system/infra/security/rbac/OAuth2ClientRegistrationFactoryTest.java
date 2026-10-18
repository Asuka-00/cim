package com.scaffold.system.infra.security.rbac;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import com.scaffold.system.domain.dto.rbac.OAuth2ProviderConfig;

class OAuth2ClientRegistrationFactoryTest {

    private final OAuth2ClientRegistrationFactory factory = new OAuth2ClientRegistrationFactory();

    @Test
    void buildManualFromEndpoints() {
        OAuth2ProviderConfig config = new OAuth2ProviderConfig();
        config.setRegistrationId("custom");
        config.setClientName("Custom IdP");
        config.setClientId("client-1");
        config.setClientSecret("secret");
        config.setScope("openid,profile");
        config.setAuthorizationUri("https://idp.example/oauth/authorize");
        config.setTokenUri("https://idp.example/oauth/token");
        config.setUserInfoUri("https://idp.example/oauth/userinfo");
        config.setUserNameAttribute("preferred_username");

        ClientRegistration registration = factory.build(config);
        assertThat(registration).isNotNull();
        assertThat(registration.getRegistrationId()).isEqualTo("custom");
        assertThat(registration.getClientId()).isEqualTo("client-1");
        assertThat(registration.getClientName()).isEqualTo("Custom IdP");
        assertThat(registration.getAuthorizationGrantType()).isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
        assertThat(registration.getProviderDetails().getAuthorizationUri())
                .isEqualTo("https://idp.example/oauth/authorize");
        assertThat(registration.getProviderDetails().getTokenUri())
                .isEqualTo("https://idp.example/oauth/token");
        assertThat(registration.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName())
                .isEqualTo("preferred_username");
        assertThat(registration.getScopes()).contains("openid", "profile");
    }

    @Test
    void buildReturnsNullWhenMissingClientId() {
        OAuth2ProviderConfig config = new OAuth2ProviderConfig();
        config.setRegistrationId("x");
        assertThat(factory.build(config)).isNull();
    }

    @Test
    void buildReturnsNullWhenNoIssuerAndNoEndpoints() {
        OAuth2ProviderConfig config = new OAuth2ProviderConfig();
        config.setRegistrationId("x");
        config.setClientId("cid");
        assertThat(factory.build(config)).isNull();
    }
}
