package com.scaffold.system.domain.service.rbac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.system.domain.dto.rbac.OAuth2ProviderConfig;
import com.scaffold.system.domain.repository.rbac.OAuth2RegistrationRepository;
import com.scaffold.system.domain.vo.rbac.OAuth2ProviderVO;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthApplicationServiceTest {

    @Mock
    private OAuth2RegistrationRepository clientRegistrationRepository;
    @Mock
    private OAuth2TicketService oauth2TicketService;

    private OAuth2AuthApplicationService service;

    @BeforeEach
    void setUp() {
        service = new OAuth2AuthApplicationService(clientRegistrationRepository, oauth2TicketService);
    }

    @Test
    void listProvidersEmptyWhenDisabled() {
        when(clientRegistrationRepository.currentSettings()).thenReturn(OAuth2Settings.defaults());
        assertThat(service.listProviders()).isEmpty();
    }

    @Test
    void listProvidersFromConfigWithoutResolvingIdP() {
        OAuth2ProviderConfig cfg = new OAuth2ProviderConfig();
        cfg.setRegistrationId("oidc");
        cfg.setClientName("OIDC");
        cfg.setEnabled(true);
        cfg.setClientId("app");
        cfg.setIssuerUri("https://idp.example.com/realms/app");

        when(clientRegistrationRepository.currentSettings()).thenReturn(
                OAuth2Settings.builder().enabled(true).userNameClaim("preferred_username")
                        .frontendSuccessUrl("s").frontendFailureUrl("f").ticketTtlSeconds(60).build());
        when(clientRegistrationRepository.listEnabledProviderConfigs()).thenReturn(List.of(cfg));

        List<OAuth2ProviderVO> providers = service.listProviders();
        assertThat(providers).hasSize(1);
        assertThat(providers.get(0).getRegistrationId()).isEqualTo("oidc");
        assertThat(providers.get(0).getDisplayName()).isEqualTo("OIDC");
        verify(clientRegistrationRepository, never()).validateRegistration(eq("oidc"));
    }

    @Test
    void requireEnabledRegistrationFailsWhenDisabled() {
        when(clientRegistrationRepository.currentSettings()).thenReturn(OAuth2Settings.defaults());
        assertThatThrownBy(() -> service.requireEnabledRegistration("oidc"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCodeConstants.OAUTH2_NOT_ENABLED);
    }

    @Test
    void requireEnabledRegistrationResolvesIdPOnClick() {
        when(clientRegistrationRepository.currentSettings()).thenReturn(
                OAuth2Settings.builder().enabled(true).userNameClaim("preferred_username")
                        .frontendSuccessUrl("s").frontendFailureUrl("f").ticketTtlSeconds(60).build());
        when(clientRegistrationRepository.isConfiguredRegistration("oidc")).thenReturn(true);
        doNothing().when(clientRegistrationRepository).validateRegistration("oidc");

        service.requireEnabledRegistration("oidc");
        verify(clientRegistrationRepository).validateRegistration("oidc");
    }

    @Test
    void requireEnabledRegistrationPropagatesIdPFailure() {
        when(clientRegistrationRepository.currentSettings()).thenReturn(
                OAuth2Settings.builder().enabled(true).userNameClaim("preferred_username")
                        .frontendSuccessUrl("s").frontendFailureUrl("f").ticketTtlSeconds(60).build());
        when(clientRegistrationRepository.isConfiguredRegistration("oidc")).thenReturn(true);
        doThrow(new BusinessException(ErrorCodeConstants.OAUTH2_IDP_UNAVAILABLE, "无法连接身份提供方"))
                .when(clientRegistrationRepository).validateRegistration("oidc");

        assertThatThrownBy(() -> service.requireEnabledRegistration("oidc"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCodeConstants.OAUTH2_IDP_UNAVAILABLE);
    }
}
