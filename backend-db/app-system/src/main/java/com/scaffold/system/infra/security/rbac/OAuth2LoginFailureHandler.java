package com.scaffold.system.infra.security.rbac;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.scaffold.system.domain.service.rbac.OAuth2Settings;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth2 登录失败：回跳前端并带 error 参数
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-07-17
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final DynamicClientRegistrationRepository clientRegistrationRepository;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        log.warn("OAuth2 authentication failed: {}", exception.getMessage());
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(OAuth2LoginSuccessHandler.SESSION_TENANT_ID);
            session.invalidate();
        }
        OAuth2Settings settings = clientRegistrationRepository.currentSettings();
        String base = settings.getFrontendFailureUrl();
        String encoded = URLEncoder.encode("oauth2_failed", StandardCharsets.UTF_8);
        int hashIdx = base.indexOf('#');
        String target;
        if (hashIdx >= 0) {
            String before = base.substring(0, hashIdx);
            String fragment = base.substring(hashIdx + 1);
            String joiner = fragment.contains("?") ? "&" : "?";
            target = before + "#" + fragment + joiner + "error=" + encoded;
        } else {
            String joiner = base.contains("?") ? "&" : "?";
            target = base + joiner + "error=" + encoded;
        }
        response.sendRedirect(target);
    }
}
