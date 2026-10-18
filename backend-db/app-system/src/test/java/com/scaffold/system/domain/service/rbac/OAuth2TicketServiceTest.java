package com.scaffold.system.domain.service.rbac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.system.domain.vo.rbac.LoginVO;
import com.scaffold.system.infra.security.rbac.DynamicClientRegistrationRepository;

/**
 * OAuth2TicketService 最小自检
 */
@ExtendWith(MockitoExtension.class)
class OAuth2TicketServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private DynamicClientRegistrationRepository clientRegistrationRepository;

    private OAuth2TicketService service;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(clientRegistrationRepository.currentSettings()).thenReturn(
                OAuth2Settings.builder()
                        .enabled(true)
                        .userNameClaim("preferred_username")
                        .frontendSuccessUrl("http://localhost:2900/#/login/oauth2/callback")
                        .frontendFailureUrl("http://localhost:2900/#/login")
                        .ticketTtlSeconds(60)
                        .build());
        service = new OAuth2TicketService(stringRedisTemplate, new ObjectMapper(), clientRegistrationRepository);
    }

    @Test
    void createAndConsumeTicket() {
        LoginVO loginVO = new LoginVO();
        loginVO.setUserName("admin");
        loginVO.setDisplayName("Admin");
        loginVO.setAccessToken("a");
        loginVO.setRefreshToken("r");
        loginVO.setAccessExpireSeconds(1800L);

        String ticket = service.createTicket(loginVO);
        assertThat(ticket).isNotBlank();

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), payloadCaptor.capture(), eq(60L), eq(TimeUnit.SECONDS));
        assertThat(keyCaptor.getValue()).contains(ticket);

        when(valueOperations.getAndDelete(anyString())).thenReturn(payloadCaptor.getValue());
        LoginVO consumed = service.consumeTicket(ticket);
        assertThat(consumed.getUserName()).isEqualTo("admin");
        assertThat(consumed.getAccessToken()).isEqualTo("a");
    }

    @Test
    void consumeMissingTicketFails() {
        when(valueOperations.getAndDelete(anyString())).thenReturn(null);
        assertThatThrownBy(() -> service.consumeTicket("missing"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCodeConstants.OAUTH2_TICKET_INVALID);
    }
}
