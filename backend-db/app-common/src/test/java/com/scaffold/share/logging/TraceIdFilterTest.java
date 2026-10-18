package com.scaffold.share.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void tearDown() {
        MdcUtils.clear();
    }

    @Test
    void generatesTraceIdWhenHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> during = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> during.set(MdcUtils.getTraceId()));

        assertNotNull(during.get());
        assertEquals(32, during.get().length());
        assertEquals(during.get(), response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
        assertNull(MdcUtils.getTraceId());
    }

    @Test
    void propagatesIncomingTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/roles");
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "abc123traceidfixedvalue00000001");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> during = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> during.set(MdcUtils.getTraceId()));

        assertEquals("abc123traceidfixedvalue00000001", during.get());
        assertEquals("abc123traceidfixedvalue00000001", response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
        assertNull(MdcUtils.getTraceId());
    }

    @Test
    void skipsSwaggerPaths() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        assertTrue(filter.shouldNotFilter(request));
    }
}
