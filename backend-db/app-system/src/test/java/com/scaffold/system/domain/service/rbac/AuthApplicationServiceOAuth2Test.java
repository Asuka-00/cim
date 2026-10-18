package com.scaffold.system.domain.service.rbac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.enums.StateEnum;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.system.domain.converter.rbac.LoginConverter;
import com.scaffold.system.domain.entity.rbac.User;
import com.scaffold.system.domain.repository.rbac.UserRepository;
import com.scaffold.system.domain.repository.rbac.UserTenantRepository;
import com.scaffold.system.domain.repository.system.TenantRepository;
import com.scaffold.system.domain.security.rbac.TokenService;
import com.scaffold.system.domain.vo.rbac.LoginVO;

/**
 * OAuth2 映射本地用户发 token 路径
 */
@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceOAuth2Test {

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

    @InjectMocks
    private AuthApplicationService authApplicationService;

    @Test
    void loginByExternalUserNameIssuesSession() {
        User user = new User();
        user.setUserName("admin");
        user.setDisplayName("Admin");
        user.setId(9L);
        user.setEnabled(StateEnum.ENABLED);
        user.setTenantId("t1");

        when(userRepository.findByUserName("admin")).thenReturn(Optional.of(user));
        when(userTenantRepository.existsByUserIdAndTenantId(9L, "t1")).thenReturn(true);
        when(userTenantRepository.findByUserId(9L)).thenReturn(java.util.List.of());
        when(tenantRepository.findAll()).thenReturn(java.util.List.of());
        when(tokenSessionService.getActiveAccessToken("admin")).thenReturn(null);
        when(tokenSessionService.getActiveRefreshToken("admin")).thenReturn(null);
        when(tokenService.generateAccessToken("admin", "t1")).thenReturn("access");
        when(tokenService.generateRefreshToken("admin", "t1")).thenReturn("refresh");
        when(tokenService.getAccessExpireSeconds()).thenReturn(1800L);

        LoginVO expected = new LoginVO();
        expected.setUserName("admin");
        when(loginConverter.toLoginVO(user, "access", "refresh", 1800L)).thenReturn(expected);

        LoginVO result = authApplicationService.loginByExternalUserName("admin", "t1");
        assertThat(result.getUserName()).isEqualTo("admin");
        verify(tokenSessionService).bindActiveTokens("admin", "access", "refresh");
        verify(sseService).onUserLogin("admin");
    }

    @Test
    void loginByExternalUserNameMissingUserFails() {
        when(userRepository.findByUserName("ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authApplicationService.loginByExternalUserName("ghost", "t1"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCodeConstants.USER_NOT_FOUND);
    }
}
