package com.scaffold.share.aop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.scaffold.share.config.LoggingProperties;
import com.scaffold.share.logging.LogConstants;
import com.scaffold.share.logging.MdcUtils;

class PerformanceLogAspectTest {

    @AfterEach
    void tearDown() {
        MdcUtils.clear();
    }

    @Test
    void shouldNotLeavePerformanceFieldsInMdc() throws Throwable {
        PerformanceLogAspect aspect = aspect();
        ProceedingJoinPoint joinPoint = joinPoint("queryUsers", "ok");

        Object result = aspect.logPerformance(joinPoint);

        assertEquals("ok", result);
        assertNull(MdcUtils.getDuration());
        assertNull(MdcUtils.getSuccess());
        assertNull(org.slf4j.MDC.get(LogConstants.CLASS_NAME));
        assertNull(org.slf4j.MDC.get(LogConstants.METHOD_NAME));
    }

    @Test
    void shouldRestoreExistingMdcValues() throws Throwable {
        MdcUtils.put(LogConstants.CLASS_NAME, "AuditController");
        MdcUtils.put(LogConstants.METHOD_NAME, "auditMethod");
        MdcUtils.put(LogConstants.DURATION, "12");
        MdcUtils.put(LogConstants.SUCCESS, "true");

        PerformanceLogAspect aspect = aspect();
        ProceedingJoinPoint joinPoint = joinPoint("queryUsers", "ok");

        aspect.logPerformance(joinPoint);

        assertEquals("AuditController", org.slf4j.MDC.get(LogConstants.CLASS_NAME));
        assertEquals("auditMethod", org.slf4j.MDC.get(LogConstants.METHOD_NAME));
        assertEquals("12", MdcUtils.getDuration());
        assertEquals("true", MdcUtils.getSuccess());
    }

    private PerformanceLogAspect aspect() {
        LoggingProperties properties = new LoggingProperties();
        properties.setPerformanceEnabled(true);
        return new PerformanceLogAspect(properties);
    }

    private ProceedingJoinPoint joinPoint(String methodName, Object result) throws Throwable {
        Signature signature = mock(Signature.class);
        when(signature.getName()).thenReturn(methodName);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getTarget()).thenReturn(new SampleService());
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn(result);
        return joinPoint;
    }

    static class SampleService {
    }
}
