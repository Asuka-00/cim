package com.scaffold.system.domain.service.rbac;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.context.SessionContextUtils;
import com.scaffold.share.enums.StateEnum;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.system.domain.converter.rbac.LoginConverter;
import com.scaffold.system.domain.dto.rbac.LoginRequestDTO;
import com.scaffold.system.domain.entity.rbac.User;
import com.scaffold.system.domain.entity.rbac.UserTenant;
import com.scaffold.system.domain.entity.system.Tenant;
import com.scaffold.system.domain.repository.rbac.UserRepository;
import com.scaffold.system.domain.repository.rbac.UserTenantRepository;
import com.scaffold.system.domain.repository.system.TenantRepository;
import com.scaffold.system.domain.security.rbac.TokenService;
import com.scaffold.system.domain.vo.rbac.LoginVO;
import com.scaffold.system.domain.vo.system.TenantSelectVO;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AuthApplicationService服务类
 * <p>
 * 负责处理AuthApplicationService相关业务编排与领域逻辑
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthApplicationService {

    private static final String DEFAULT_TENANT_ID = CurrentSessionProvider.DEFAULT_TENANT_ID;

    private final UserRepository userRepository;
    private final UserTenantRepository userTenantRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LoginConverter loginConverter;
    private final TokenSessionService tokenSessionService;
    private final SseService sseService;
    private final CurrentSessionProvider currentSessionProvider;

    /**
     * 处理登录请求
     */
    public LoginVO login(LoginRequestDTO request) {
        User user = userRepository.findByUserName(request.getUserName())
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.USER_NOT_FOUND,
                        "用户不存在: " + request.getUserName()));

        Objects.requireNonNull(user.getUserName(), "userName must not be null");

        if (user.getEnabled() != StateEnum.ENABLED) {
            throw new BusinessException(ErrorCodeConstants.USER_DISABLED, "用户已被禁用: " + request.getUserName());
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCodeConstants.USER_PASSWORD_INCORRECT, "密码错误");
        }

        requireMembership(user, request.getTenantId());
        return issueLoginSession(user, request.getTenantId());
    }

    /**
     * 外部身份登录（OAuth2 等）：仅映射本地已有用户。
     */
    public LoginVO loginByExternalUserName(String externalUserName, String tenantId) {
        if (externalUserName == null || externalUserName.isBlank()) {
            throw new BusinessException(ErrorCodeConstants.OAUTH2_USER_MAPPING_FAILED, "OAuth2 用户标识为空");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "租户不能为空");
        }
        User user = userRepository.findByUserName(externalUserName)
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.USER_NOT_FOUND,
                        "用户不存在: " + externalUserName));
        if (user.getEnabled() != StateEnum.ENABLED) {
            throw new BusinessException(ErrorCodeConstants.USER_DISABLED, "用户已被禁用: " + externalUserName);
        }
        requireMembership(user, tenantId);
        return issueLoginSession(user, tenantId);
    }

    /**
     * 切换当前租户并重签会话。
     */
    public LoginVO switchTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "租户不能为空");
        }
        String userName = SessionContextUtils.getCurrentUserName(currentSessionProvider);
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.USER_NOT_FOUND, "用户不存在: " + userName));
        if (user.getEnabled() != StateEnum.ENABLED) {
            throw new BusinessException(ErrorCodeConstants.USER_DISABLED, "用户已被禁用: " + userName);
        }
        requireMembership(user, tenantId);
        return issueLoginSession(user, tenantId);
    }

    /**
     * 签发本系统 access/refresh 并绑定会话（密码登录与 OAuth2 共用）。
     */
    public LoginVO issueLoginSession(User user, String tenantId) {
        String userName = Objects.requireNonNull(user.getUserName(), "userName must not be null");
        String resolvedTenantId = resolveTenantId(tenantId, user);

        String oldAccess = tokenSessionService.getActiveAccessToken(userName);
        String oldRefresh = tokenSessionService.getActiveRefreshToken(userName);
        if (oldAccess != null) {
            tokenSessionService.blacklistToken(oldAccess);
        }
        if (oldRefresh != null) {
            tokenSessionService.blacklistToken(oldRefresh);
        }

        String accessToken = tokenService.generateAccessToken(userName, resolvedTenantId);
        String refreshToken = tokenService.generateRefreshToken(userName, resolvedTenantId);
        tokenSessionService.bindActiveTokens(userName, accessToken, refreshToken);

        sseService.onUserLogin(userName);

        LoginVO loginVO = loginConverter.toLoginVO(user, accessToken, refreshToken,
                tokenService.getAccessExpireSeconds());
        fillTenantContext(loginVO, user, resolvedTenantId);
        log.info("User login succeeded: userName={}, tenantId={}", userName, resolvedTenantId);
        return loginVO;
    }

    private void requireMembership(User user, String tenantId) {
        if (PlatformAdminSupport.isPlatformAdmin(user)) {
            return;
        }
        if (user.getId() == null
                || !userTenantRepository.existsByUserIdAndTenantId(user.getId(), tenantId)) {
            throw new BusinessException(ErrorCodeConstants.USER_NOT_IN_TENANT, "用户不属于该租户");
        }
    }

    private void fillTenantContext(LoginVO loginVO, User user, String tenantId) {
        loginVO.setTenantId(tenantId);
        loginVO.setPlatformAdmin(PlatformAdminSupport.isPlatformAdmin(user));
        if (PlatformAdminSupport.isPlatformAdmin(user)) {
            loginVO.setTenants(toEnabledTenantSelectList());
            return;
        }
        loginVO.setTenants(toTenantSelectList(userTenantRepository.findByUserId(user.getId())));
    }

    private List<TenantSelectVO> toEnabledTenantSelectList() {
        List<TenantSelectVO> result = new ArrayList<>();
        for (Tenant tenant : tenantRepository.findAll()) {
            if (!StateEnum.ENABLED.equals(tenant.getEnabled()) || tenant.getId() == null) {
                continue;
            }
            TenantSelectVO vo = new TenantSelectVO();
            vo.setId(String.valueOf(tenant.getId()));
            vo.setTenantId(tenant.getTenantId());
            vo.setTenantName(tenant.getTenantName());
            result.add(vo);
        }
        return result;
    }

    private List<TenantSelectVO> toTenantSelectList(List<UserTenant> memberships) {
        Map<String, Tenant> tenantByPk = new LinkedHashMap<>();
        for (Tenant tenant : tenantRepository.findAll()) {
            if (tenant.getId() != null) {
                tenantByPk.put(String.valueOf(tenant.getId()), tenant);
            }
        }
        List<TenantSelectVO> result = new ArrayList<>();
        for (UserTenant membership : memberships) {
            TenantSelectVO vo = new TenantSelectVO();
            vo.setId(membership.getTenantId());
            Tenant tenant = tenantByPk.get(membership.getTenantId());
            if (tenant != null) {
                vo.setTenantId(tenant.getTenantId());
                vo.setTenantName(tenant.getTenantName());
            }
            result.add(vo);
        }
        return result;
    }

    private String resolveTenantId(String requestTenantId, User user) {
        if (requestTenantId != null && !requestTenantId.isBlank()) {
            return requestTenantId;
        }
        String tenantId = user.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return DEFAULT_TENANT_ID;
        }
        return tenantId;
    }

    /**
     * 处理登出请求
     */
    public void logout(String accessToken) {
        String userName = Objects.requireNonNull(tokenService.getUserName(accessToken),
                "userName must not be null");
        tokenSessionService.blacklistToken(accessToken);

        String activeAccess = tokenSessionService.getActiveAccessToken(userName);
        if (accessToken.equals(activeAccess)) {
            tokenSessionService.removeActiveAccessToken(userName);
        }

        sseService.onUserLogout(userName);
        log.info("User logout succeeded: userName={}", userName);
    }

    /**
     * 刷新访问令牌
     */
    public LoginVO refreshAccessToken(String refreshToken) {
        try {
            if (!TokenService.TOKEN_TYPE_REFRESH.equals(tokenService.getTokenType(refreshToken))) {
                throw new BusinessException(ErrorCodeConstants.TOKEN_TYPE_INVALID, "token类型错误: 非refresh token");
            }

            if (tokenSessionService.isBlacklisted(refreshToken)) {
                throw new BusinessException(ErrorCodeConstants.TOKEN_BLACKLISTED, "refresh token已失效");
            }

            String userName = tokenService.getUserName(refreshToken);
            String tenantId = tokenService.getTenantId(refreshToken);
            if (tenantId == null || tenantId.isBlank()) {
                tenantId = DEFAULT_TENANT_ID;
            }
            String activeRefresh = tokenSessionService.getActiveRefreshToken(userName);
            if (!refreshToken.equals(activeRefresh)) {
                throw new BusinessException(ErrorCodeConstants.TOKEN_SESSION_INVALID, "refresh token不是当前有效会话");
            }

            String oldAccess = tokenSessionService.getActiveAccessToken(userName);
            if (oldAccess != null) {
                tokenSessionService.blacklistToken(oldAccess);
            }

            String newAccess = tokenService.generateAccessToken(userName, tenantId);
            tokenSessionService.updateActiveAccessToken(userName, newAccess);

            User user = userRepository.findByUserName(userName)
                    .orElseThrow(
                            () -> new BusinessException(ErrorCodeConstants.USER_NOT_FOUND, "用户不存在: " + userName));

            LoginVO loginVO = loginConverter.toLoginVO(user, newAccess, refreshToken,
                    tokenService.getAccessExpireSeconds());
            fillTenantContext(loginVO, user, tenantId);
            log.info("Access token refreshed: userName={}, tenantId={}", userName, tenantId);
            return loginVO;
        } catch (JwtException ex) {
            throw new BusinessException(ErrorCodeConstants.TOKEN_INVALID_OR_EXPIRED, "refresh token无效或已过期", ex);
        }
    }
}
