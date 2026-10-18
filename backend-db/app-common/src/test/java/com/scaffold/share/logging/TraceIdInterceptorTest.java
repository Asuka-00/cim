package com.scaffold.share.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.scaffold.share.context.CurrentSessionProvider;

class TraceIdInterceptorTest {

    @AfterEach
    void tearDown() {
        MdcUtils.clear();
    }

    @Test
    void shouldUseSessionIdentityBeforeHeaders() {
        TraceIdInterceptor interceptor = new TraceIdInterceptor(session("u001", "t001"));
        MockHttpServletRequest request = request();
        request.addHeader("X-User-Code", "header-user");
        request.addHeader("X-Tenant-Id", "header-tenant");

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertEquals("u001", MdcUtils.getUserName());
        assertEquals("t001", MdcUtils.getTenantId());
        assertNotNull(MdcUtils.getTraceId());
    }

    @Test
    void shouldUseHeadersWhenSessionIdentityMissing() {
        TraceIdInterceptor interceptor = new TraceIdInterceptor(session(null, null));
        MockHttpServletRequest request = request();
        request.addHeader("X-User-Code", "header-user");
        request.addHeader("X-Tenant-Id", "header-tenant");

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertEquals("header-user", MdcUtils.getUserName());
        assertEquals("header-tenant", MdcUtils.getTenantId());
    }

    @Test
    void shouldUseDefaultTenantOnlyWhenSessionAndHeaderMissing() {
        TraceIdInterceptor interceptor = new TraceIdInterceptor(session(null, null));

        interceptor.preHandle(request(), new MockHttpServletResponse(), new Object());

        assertNull(MdcUtils.getUserName());
        assertEquals(CurrentSessionProvider.DEFAULT_TENANT_ID, MdcUtils.getTenantId());
    }

    @Test
    void shouldNotClearMdcOnAfterCompletion() throws Exception {
        // 清理由 TraceIdFilter 统一负责，避免与 Filter finally 时序冲突
        TraceIdInterceptor interceptor = new TraceIdInterceptor(session("u001", "t001"));
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);

        assertEquals("u001", MdcUtils.getUserName());
        assertEquals("t001", MdcUtils.getTenantId());
        assertNotNull(MdcUtils.getTraceId());
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.setRemoteAddr("127.0.0.1");
        return request;
    }

    private CurrentSessionProvider session(String userName, String tenantId) {
        return new CurrentSessionProvider() {
            @Override
            public String getCurrentUserName() {
                return userName;
            }

            @Override
            public String getCurrentTenantId() {
                return tenantId;
            }
        };
    }
}
