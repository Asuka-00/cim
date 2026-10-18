package com.scaffold.system.infra.security.rbac;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.context.SessionPrincipal;
import com.scaffold.share.web.ApiErrorResponseWriter;
import com.scaffold.system.domain.service.rbac.TokenSessionService;
import com.scaffold.system.domain.security.rbac.TokenService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JwtAuthenticationFilter过滤器类
 * <p>
 * 负责处理JwtAuthenticationFilter相关请求过滤与预处理逻辑
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String DEFAULT_TENANT_ID = CurrentSessionProvider.DEFAULT_TENANT_ID;

    @Value("${auth.jwt.test-token:}")
    private String testToken;

    private final ApiErrorResponseWriter apiErrorResponseWriter;
    private final TokenService tokenService;
    private final TokenSessionService tokenSessionService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7);

        // 测试 token 跳过验证
        if (testToken != null && !testToken.isEmpty() && testToken.equals(token)) {
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    new SessionPrincipal("test-user", DEFAULT_TENANT_ID),
                    null,
                    authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (!TokenService.TOKEN_TYPE_ACCESS.equals(tokenService.getTokenType(token))) {
                apiErrorResponseWriter.write(response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        ErrorCodeConstants.TOKEN_TYPE_INVALID,
                        "token类型错误");
                return;
            }

            if (tokenSessionService.isBlacklisted(token)) {
                apiErrorResponseWriter.write(response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        ErrorCodeConstants.TOKEN_BLACKLISTED,
                        "token已失效");
                return;
            }

            String userName = tokenService.getUserName(token);
            String tenantId = tokenService.getTenantId(token);
            if (tenantId == null || tenantId.isBlank()) {
                tenantId = DEFAULT_TENANT_ID;
            }
            String activeAccess = tokenSessionService.getActiveAccessToken(userName);
            if (activeAccess == null || !token.equals(activeAccess)) {
                apiErrorResponseWriter.write(response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        ErrorCodeConstants.TOKEN_SESSION_INVALID,
                        "token不是当前有效会话");
                return;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    new SessionPrincipal(userName, tenantId),
                    null,
                    Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException ex) {
            apiErrorResponseWriter.write(response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    ErrorCodeConstants.TOKEN_INVALID_OR_EXPIRED,
                    "token无效或已过期");
        }
    }
}
