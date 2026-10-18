package com.scaffold.system.api.v1.system;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.scaffold.share.web.ApiErrorResponseWriter;
import com.scaffold.system.domain.service.system.QpsTestApplicationService;
import com.scaffold.system.domain.service.rbac.TokenSessionService;
import com.scaffold.system.infra.config.rbac.SecurityConfig;
import com.scaffold.system.infra.security.rbac.DynamicClientRegistrationRepository;
import com.scaffold.system.infra.security.rbac.JwtAuthenticationFilter;
import com.scaffold.system.infra.security.rbac.JwtTokenProvider;
import com.scaffold.system.infra.security.rbac.OAuth2LoginFailureHandler;
import com.scaffold.system.infra.security.rbac.OAuth2LoginSuccessHandler;

@WebMvcTest(QpsTestControllerV1.class)
@ContextConfiguration(classes = QpsTestControllerV1.class)
@Import({
                SecurityConfig.class,
                JwtAuthenticationFilter.class,
                ApiErrorResponseWriter.class
})
class QpsTestControllerV1Test {

        @Autowired
        private MockMvc mockMvc;

        @SuppressWarnings("removal")
        @MockBean
        private QpsTestApplicationService qpsTestApplicationService;

        @SuppressWarnings("removal")
        @MockBean
        private JwtTokenProvider jwtTokenProvider;

        @SuppressWarnings("removal")
        @MockBean
        private TokenSessionService tokenSessionService;

        @SuppressWarnings("removal")
        @MockBean
        private OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;

        @SuppressWarnings("removal")
        @MockBean
        private OAuth2LoginFailureHandler oauth2LoginFailureHandler;

        @SuppressWarnings("removal")
        @MockBean
        private DynamicClientRegistrationRepository clientRegistrationRepository;

        @Test
        void pingCanBeAccessedWithoutAuthentication() throws Exception {
                when(qpsTestApplicationService.ping()).thenReturn(Map.of(
                                "timestamp", LocalDateTime.now(),
                                "requestId", "request-id",
                                "server", "node-01",
                                "version", "0.0.1-SNAPSHOT",
                                "hotStore", "db"));

                mockMvc.perform(get("/api/v1/test/ping"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("success"))
                                .andExpect(jsonPath("$.data.requestId").value("request-id"))
                                .andExpect(jsonPath("$.data.hotStore").value("db"));
        }

        @Test
        void mediumCanBeAccessedWithoutAuthentication() throws Exception {
                when(qpsTestApplicationService.medium()).thenReturn(Map.of(
                                "timestamp", LocalDateTime.now(),
                                "requestId", "request-id",
                                "hotStore", "db",
                                "products", List.of(Map.of("id", 1, "code", "QPS-PRODUCT-1"))));

                mockMvc.perform(get("/api/v1/test/medium"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.products.length()").value(1));
        }
}
