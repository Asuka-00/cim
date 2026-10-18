package com.scaffold.system.infra.security.rbac;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.system.domain.service.rbac.AuthApplicationService;
import com.scaffold.system.domain.service.rbac.OAuth2Settings;
import com.scaffold.system.domain.service.rbac.OAuth2TicketService;
import com.scaffold.system.domain.vo.rbac.LoginVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth2 登录成功：映射本地用户、签发本系统 JWT、ticket 回跳前端
 * <p>
 * IdP token 仅用于握手，不回传前端；业务会话一律为本系统 JWT。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-07-17
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    public static final String SESSION_TENANT_ID = "OAUTH2_LOGIN_TENANT_ID";

    private final AuthApplicationService authApplicationService;
    private final OAuth2TicketService oauth2TicketService;
    private final DynamicClientRegistrationRepository clientRegistrationRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        try {
            if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
                redirectFailure(response, "unsupported_auth");
                return;
            }
            String tenantId = resolveTenantId(request);
            if (tenantId == null || tenantId.isBlank()) {
                redirectFailure(response, "tenant_required");
                return;
            }
            OAuth2User oauth2User = oauthToken.getPrincipal();
            String userName = resolveUserName(oauthToken.getAuthorizedClientRegistrationId(), oauth2User);
            LoginVO loginVO = authApplicationService.loginByExternalUserName(userName, tenantId);
            String ticket = oauth2TicketService.createTicket(loginVO);
            clearOAuthSession(request);
            OAuth2Settings settings = clientRegistrationRepository.currentSettings();
            String target = appendQuery(settings.getFrontendSuccessUrl(), "ticket", ticket);
            response.sendRedirect(target);
        } catch (BusinessException ex) {
            log.warn("OAuth2 login business failed: code={}, message={}", ex.getCode(), ex.getMessage());
            clearOAuthSession(request);
            redirectFailure(response, mapError(ex.getCode()));
        } catch (Exception ex) {
            log.error("OAuth2 login failed", ex);
            clearOAuthSession(request);
            redirectFailure(response, "oauth2_failed");
        }
    }

    private String resolveTenantId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_TENANT_ID);
        return value == null ? null : String.valueOf(value);
    }

    private String resolveUserName(String registrationId, OAuth2User oauth2User) {
        OAuth2Settings settings = clientRegistrationRepository.currentSettings();
        String claim = null;
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(registrationId);
        if (registration != null
                && registration.getProviderDetails() != null
                && registration.getProviderDetails().getUserInfoEndpoint() != null) {
            claim = registration.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        }
        if (claim == null || claim.isBlank()) {
            claim = settings.getUserNameClaim();
        }
        if (claim == null || claim.isBlank()) {
            claim = "preferred_username";
        }
        Map<String, Object> attributes = oauth2User.getAttributes();
        Object value = attributes.get(claim);
        if (value == null || String.valueOf(value).isBlank()) {
            value = attributes.get("preferred_username");
        }
        if (value == null || String.valueOf(value).isBlank()) {
            value = attributes.get("sub");
        }
        if (value == null || String.valueOf(value).isBlank()) {
            throw new BusinessException(ErrorCodeConstants.OAUTH2_USER_MAPPING_FAILED, "无法从 IdP 解析用户名");
        }
        return String.valueOf(value).trim();
    }

    private void clearOAuthSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SESSION_TENANT_ID);
            session.invalidate();
        }
    }

    private void redirectFailure(HttpServletResponse response, String error) throws IOException {
        OAuth2Settings settings = clientRegistrationRepository.currentSettings();
        String target = appendQuery(settings.getFrontendFailureUrl(), "error", error);
        response.sendRedirect(target);
    }

    private String mapError(int code) {
        if (code == ErrorCodeConstants.USER_NOT_FOUND) {
            return "user_not_found";
        }
        if (code == ErrorCodeConstants.USER_DISABLED) {
            return "user_disabled";
        }
        if (code == ErrorCodeConstants.OAUTH2_USER_MAPPING_FAILED) {
            return "mapping_failed";
        }
        return "oauth2_failed";
    }

    private String appendQuery(String baseUrl, String name, String value) {
        String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8);
        // hash 路由：query 需落在 # 之前或 fragment 内；前端约定 success URL 含 #/path，则写到 fragment
        int hashIdx = baseUrl.indexOf('#');
        if (hashIdx >= 0) {
            String before = baseUrl.substring(0, hashIdx);
            String fragment = baseUrl.substring(hashIdx + 1);
            String joiner = fragment.contains("?") ? "&" : "?";
            return before + "#" + fragment + joiner + name + "=" + encoded;
        }
        return UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam(name, value)
                .build(true)
                .toUriString();
    }
}
