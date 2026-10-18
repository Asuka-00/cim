package com.scaffold.system.infra.config.rbac;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.web.ApiErrorResponseWriter;
import com.scaffold.system.infra.security.rbac.DynamicClientRegistrationRepository;
import com.scaffold.system.infra.security.rbac.JwtAuthenticationFilter;
import com.scaffold.system.infra.security.rbac.JwtProperties;
import com.scaffold.system.infra.security.rbac.OAuth2LoginFailureHandler;
import com.scaffold.system.infra.security.rbac.OAuth2LoginSuccessHandler;

/**
 * SecurityConfig配置类
 * <p>
 * 负责提供SecurityConfig相关配置与组件装配。
 * OAuth2 Client registration 来自系统配置表（{@link DynamicClientRegistrationRepository}），
 * IdP 仅做身份认证，业务 API 仍使用本系统 JWT。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */

@Configuration
@EnableConfigurationProperties({ JwtProperties.class })
public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http,
                        JwtAuthenticationFilter jwtAuthenticationFilter,
                        AuthenticationEntryPoint unauthorizedEntryPoint,
                        AccessDeniedHandler accessDeniedHandler,
                        OAuth2LoginSuccessHandler oauth2LoginSuccessHandler,
                        OAuth2LoginFailureHandler oauth2LoginFailureHandler,
                        DynamicClientRegistrationRepository clientRegistrationRepository)
                        throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                // OAuth2 授权码握手需要短暂会话；API 仍靠 JWT
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/", "/index.html", "/favicon.ico",
                                                                "/platform-config.json", "/brand/**",
                                                                "/static/**",
                                                                "/swagger-ui.html", "/swagger-ui/**",
                                                                "/api/v1/auth/login", "/api/v1/auth/refresh",
                                                                "/api/v1/auth/brand",
                                                                "/api/v1/auth/oauth2/**",
                                                                "/oauth2/**", "/login/oauth2/**",
                                                                "/api/v1/auth/sse/events",
                                                                "/api/v1/test/**",
                                                                "/api/v1/tenants/dropdown",
                                                                "/api/actuator/health", "/api/actuator/info",
                                                                "/api/actuator/prometheus",
                                                                "/v3/api-docs/**")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(unauthorizedEntryPoint)
                                                .accessDeniedHandler(accessDeniedHandler))
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                                // 始终挂 OAuth2 登录；厂商列表由 DynamicClientRegistrationRepository 热加载
                                .oauth2Login(oauth2 -> oauth2
                                                .clientRegistrationRepository(clientRegistrationRepository)
                                                .successHandler(oauth2LoginSuccessHandler)
                                                .failureHandler(oauth2LoginFailureHandler));
                return http.build();
        }

        /**
         * 创建密码编码器
         */
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        /**
         * 创建未授权入口处理器
         */
        @Bean
        public AuthenticationEntryPoint unauthorizedEntryPoint(ApiErrorResponseWriter apiErrorResponseWriter) {
                return (request, response, authException) -> {
                        apiErrorResponseWriter.write(response,
                                        HttpStatus.UNAUTHORIZED.value(),
                                        ErrorCodeConstants.AUTHENTICATION_REQUIRED,
                                        "未登录或登录已过期");
                };
        }

        /**
         * 创建访问拒绝处理器
         */
        @Bean
        public AccessDeniedHandler accessDeniedHandler(ApiErrorResponseWriter apiErrorResponseWriter) {
                return (request, response, accessDeniedException) -> {
                        apiErrorResponseWriter.write(response,
                                        HttpStatus.FORBIDDEN.value(),
                                        ErrorCodeConstants.ACCESS_DENIED,
                                        "无访问权限");
                };
        }
}
