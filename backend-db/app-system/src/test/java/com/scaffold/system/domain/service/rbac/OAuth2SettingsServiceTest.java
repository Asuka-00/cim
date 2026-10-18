package com.scaffold.system.domain.service.rbac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.share.enums.StateEnum;
import com.scaffold.system.domain.entity.system.SystemConfig;
import com.scaffold.system.domain.repository.system.SystemConfigRepository;

@ExtendWith(MockitoExtension.class)
class OAuth2SettingsServiceTest {

    @Mock
    private SystemConfigRepository systemConfigRepository;

    private OAuth2SettingsService service;

    @BeforeEach
    void setUp() {
        service = new OAuth2SettingsService(systemConfigRepository, new ObjectMapper());
    }

    @Test
    void loadDefaultsWhenEmpty() {
        when(systemConfigRepository.findAllByGroupCodeIgnoreTenant(eq(OAuth2SettingsService.GROUP_CODE)))
                .thenReturn(List.of());
        OAuth2Settings settings = service.load();
        assertThat(settings.isEnabled()).isFalse();
        assertThat(settings.getTicketTtlSeconds()).isEqualTo(60);
        assertThat(settings.getProviders()).isEmpty();
    }

    @Test
    void loadGlobalAndProviders() {
        SystemConfig enabled = row("oauth2.enabled", "true", "boolean");
        SystemConfig ttl = row("oauth2.ticket-ttl-seconds", "90", "int");
        SystemConfig provider = row("oauth2.provider.keycloak",
                "{\"registrationId\":\"keycloak\",\"clientName\":\"KC\",\"enabled\":true,\"clientId\":\"rtd\",\"issuerUri\":\"https://idp.example/realms/app\"}",
                "json");
        SystemConfig disabledProvider = row("oauth2.provider.disabled",
                "{\"registrationId\":\"disabled\",\"enabled\":false,\"clientId\":\"x\"}",
                "json");
        when(systemConfigRepository.findAllByGroupCodeIgnoreTenant(eq(OAuth2SettingsService.GROUP_CODE)))
                .thenReturn(List.of(enabled, ttl, provider, disabledProvider));

        OAuth2Settings settings = service.load();
        assertThat(settings.isEnabled()).isTrue();
        assertThat(settings.getTicketTtlSeconds()).isEqualTo(90);
        assertThat(settings.getProviders()).hasSize(2);
        assertThat(settings.getProviders().get(0).getRegistrationId()).isEqualTo("keycloak");
        assertThat(settings.getProviders().get(0).isEnabled()).isTrue();
        assertThat(settings.getProviders().get(1).isEnabled()).isFalse();
    }

    @Test
    void registrationIdFollowsConfigCodeSuffix() {
        SystemConfig provider = row("oauth2.provider.azure",
                "{\"registrationId\":\"wrong\",\"clientId\":\"cid\",\"enabled\":true}",
                "json");
        when(systemConfigRepository.findAllByGroupCodeIgnoreTenant(eq(OAuth2SettingsService.GROUP_CODE)))
                .thenReturn(List.of(provider));
        OAuth2Settings settings = service.load();
        assertThat(settings.getProviders()).hasSize(1);
        assertThat(settings.getProviders().get(0).getRegistrationId()).isEqualTo("azure");
    }

    private static SystemConfig row(String code, String value, String type) {
        SystemConfig c = new SystemConfig();
        c.setConfigCode(code);
        c.setConfigValue(value);
        c.setValueType(type);
        c.setGroupCode(OAuth2SettingsService.GROUP_CODE);
        c.setState(StateEnum.ENABLED);
        return c;
    }
}
