package com.scaffold.system.api.v1.log;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.scaffold.share.web.ApiErrorResponseWriter;
import com.scaffold.system.domain.log.LogFileService;
import com.scaffold.system.domain.log.LogTailVO;
import com.scaffold.system.domain.service.rbac.TokenSessionService;
import com.scaffold.system.infra.config.rbac.SecurityConfig;
import com.scaffold.system.infra.security.rbac.DynamicClientRegistrationRepository;
import com.scaffold.system.infra.security.rbac.JwtAuthenticationFilter;
import com.scaffold.system.infra.security.rbac.JwtTokenProvider;
import com.scaffold.system.infra.security.rbac.OAuth2LoginFailureHandler;
import com.scaffold.system.infra.security.rbac.OAuth2LoginSuccessHandler;

@WebMvcTest(LogFileControllerV1.class)
@ContextConfiguration(classes = LogFileControllerV1.class)
@Import({
                SecurityConfig.class,
                JwtAuthenticationFilter.class,
                ApiErrorResponseWriter.class
})
@TestPropertySource(properties = "auth.jwt.test-token=test-token")
class LogFileControllerV1Test {

        @Autowired
        private MockMvc mockMvc;

        @SuppressWarnings("removal")
        @MockBean
        private LogFileService logFileService;

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
        void logFileEndpointsRequireAuthentication() throws Exception {
                mockMvc.perform(get("/api/v1/audit/log-files")
                                .param("type", "audit"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void tailAcceptsAllLinesParameter() throws Exception {
                when(logFileService.tail(eq("audit"), eq("audit.log"), eq("ALL")))
                                .thenReturn(new LogTailVO("audit.log", "audit", 2, List.of("one", "two")));

                mockMvc.perform(get("/api/v1/audit/log-files/audit/tail")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                                .param("fileName", "audit.log")
                                .param("lines", "ALL"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.lines").value(2))
                                .andExpect(jsonPath("$.data.content[0]").value("one"))
                                .andExpect(jsonPath("$.data.content[1]").value("two"));

                verify(logFileService).tail("audit", "audit.log", "ALL");
        }

        @Test
        void downloadReturnsAttachmentHeadersAndContent() throws Exception {
                Resource resource = new ByteArrayResource("hello".getBytes(StandardCharsets.UTF_8)) {
                        @Override
                        public String getFilename() {
                                return "audit.log";
                        }
                };
                when(logFileService.download(eq("audit"), eq("audit.log"))).thenReturn(resource);
                when(logFileService.contentLength(any(Resource.class))).thenReturn(5L);

                mockMvc.perform(get("/api/v1/audit/log-files/audit/download")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                                .param("fileName", "audit.log"))
                                .andExpect(status().isOk())
                                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/octet-stream"))
                                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, "5"))
                                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                                                containsString("attachment")))
                                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                                                containsString("audit.log")));
        }
}
