package com.scaffold.system.domain.service.rbac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.enums.StateEnum;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.system.domain.converter.rbac.LoginConverter;
import com.scaffold.system.domain.dto.rbac.LoginRequestDTO;
import com.scaffold.system.domain.entity.rbac.User;
import com.scaffold.system.domain.repository.rbac.UserRepository;
import com.scaffold.system.domain.repository.rbac.UserTenantRepository;
import com.scaffold.system.domain.repository.system.TenantRepository;
import com.scaffold.system.domain.security.rbac.TokenService;
import com.scaffold.system.domain.vo.rbac.LoginVO;

@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserTenantRepository userTenantRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenService tokenService;
    @Mock
    private LoginConverter loginConverter;
    @Mock
    private TokenSessionService tokenSessionService;
    @Mock
    private SseService sseService;
    @Mock
    private CurrentSessionProvider currentSessionProvider;

    @InjectMocks
    private AuthApplicationService authApplicationService;

    @Test
    void loginFailsWhenUserHasNoMembership() {
        User user = enabledUser();
        LoginRequestDTO request = new LoginRequestDTO();
        request.setUserName("admin");
        request.setPassword("secret");
        request.setTenantId("t2");

        when(userRepository.findByUserName("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(userTenantRepository.existsByUserIdAndTenantId(9L, "t2")).thenReturn(false);

        assertThatThrownBy(() -> authApplicationService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCodeConstants.USER_NOT_IN_TENANT);
    }

    @Test
    void switchTenantSucceedsWhenMembershipExists() {
        User user = enabledUser();
        when(currentSessionProvider.getCurrentUserName()).thenReturn("admin");
        when(userRepository.findByUserName("admin")).thenReturn(Optional.of(user));
        when(userTenantRepository.existsByUserIdAndTenantId(9L, "t2")).thenReturn(true);
        when(userTenantRepository.findByUserId(9L)).thenReturn(List.of());
        when(tenantRepository.findAll()).thenReturn(List.of());
        when(tokenService.generateAccessToken("admin", "t2")).thenReturn("access");
        when(tokenService.generateRefreshToken("admin", "t2")).thenReturn("refresh");
        when(tokenService.getAccessExpireSeconds()).thenReturn(1800L);
        LoginVO expected = new LoginVO();
        when(loginConverter.toLoginVO(user, "access", "refresh", 1800L)).thenReturn(expected);

        LoginVO result = authApplicationService.switchTenant("t2");

        assertThat(result.getTenantId()).isEqualTo("t2");
        verify(tokenSessionService).bindActiveTokens("admin", "access", "refresh");
    }

    @Test
    void switchTenantSucceedsForPlatformAdminWithoutMembership() {
        User user = enabledUser();
        user.setPlatformAdmin(true);
        when(currentSessionProvider.getCurrentUserName()).thenReturn("admin");
        when(userRepository.findByUserName("admin")).thenReturn(Optional.of(user));
        when(tenantRepository.findAll()).thenReturn(List.of());
        when(tokenService.generateAccessToken("admin", "t9")).thenReturn("access");
        when(tokenService.generateRefreshToken("admin", "t9")).thenReturn("refresh");
        when(tokenService.getAccessExpireSeconds()).thenReturn(1800L);
        LoginVO expected = new LoginVO();
        when(loginConverter.toLoginVO(user, "access", "refresh", 1800L)).thenReturn(expected);

        LoginVO result = authApplicationService.switchTenant("t9");

        assertThat(result.getTenantId()).isEqualTo("t9");
        assertThat(result.getPlatformAdmin()).isTrue();
        verify(userTenantRepository, never()).existsByUserIdAndTenantId(anyLong(), anyString());
        verify(tokenSessionService).bindActiveTokens("admin", "access", "refresh");
    }

    @Test
    void switchTenantFailsWithoutMembership() {
        User user = enabledUser();
        when(currentSessionProvider.getCurrentUserName()).thenReturn("admin");
        when(userRepository.findByUserName("admin")).thenReturn(Optional.of(user));
        when(userTenantRepository.existsByUserIdAndTenantId(9L, "t9")).thenReturn(false);

        assertThatThrownBy(() -> authApplicationService.switchTenant("t9"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCodeConstants.USER_NOT_IN_TENANT);
    }

    private User enabledUser() {
        User user = new User();
        user.setId(9L);
        user.setUserName("admin");
        user.setDisplayName("Admin");
        user.setEnabled(StateEnum.ENABLED);
        user.setPasswordHash("hash");
        user.setTenantId("t1");
        return user;
    }
}
