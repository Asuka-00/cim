package com.scaffold.share.aop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.share.audit.OperationLog;
import com.scaffold.share.audit.OperationLogService;
import com.scaffold.share.config.LoggingProperties;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.logging.AuditPayloadSerializer;
import com.scaffold.share.logging.MdcUtils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class AuditLogAspectTest {

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        MdcUtils.clear();
        Logger auditLogger = (Logger) LoggerFactory.getLogger("AUDIT");
        listAppender = new ListAppender<>();
        listAppender.start();
        auditLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        Logger auditLogger = (Logger) LoggerFactory.getLogger("AUDIT");
        auditLogger.detachAppender(listAppender);
        MdcUtils.clear();
    }

    @Test
    void shouldIncludeUserNameInRequestAndResponseLogs() throws Throwable {
        MdcUtils.setUserName("u001");
        OperationLogService operationLogService = mock(OperationLogService.class);
        AuditLogAspect aspect = aspect(operationLogService);
        ProceedingJoinPoint joinPoint = joinPoint("success", "ok");

        Object result = aspect.audit(joinPoint);

        assertEquals("ok", result);
        assertEquals(2, listAppender.list.size());
        assertEquals("AUDIT_API_REQUEST", listAppender.list.get(0).getFormattedMessage());
        assertEquals("AUDIT_API_RESPONSE", listAppender.list.get(1).getFormattedMessage());
        assertStructuredArgument(listAppender.list.get(0), "userName", "u001");
        assertStructuredArgument(listAppender.list.get(1), "userName", "u001");

        OperationLog operationLog = captureOperationLog(operationLogService);
        assertEquals("成功操作", operationLog.getOperation());
        assertEquals("u001", operationLog.getUserName());
        assertEquals("SampleController", operationLog.getClassName());
        assertEquals("success", operationLog.getMethodName());
        assertTrue(operationLog.getSuccess());
        assertNotNull(operationLog.getTraceId());
        assertNotNull(operationLog.getRequestPayload());
        assertNotNull(operationLog.getResponsePayload());
        assertNotNull(operationLog.getDurationMs());
    }

    @Test
    void shouldIncludeUserNameInErrorLogs() throws Throwable {
        MdcUtils.setUserName("u001");
        OperationLogService operationLogService = mock(OperationLogService.class);
        AuditLogAspect aspect = aspect(operationLogService);
        RuntimeException failure = new RuntimeException("boom");
        ProceedingJoinPoint joinPoint = joinPoint("failure", failure);

        assertThrows(RuntimeException.class, () -> aspect.audit(joinPoint));

        assertEquals(2, listAppender.list.size());
        assertEquals("AUDIT_API_REQUEST", listAppender.list.get(0).getFormattedMessage());
        assertEquals("AUDIT_API_ERROR", listAppender.list.get(1).getFormattedMessage());
        assertStructuredArgument(listAppender.list.get(1), "userName", "u001");

        OperationLog operationLog = captureOperationLog(operationLogService);
        assertEquals("失败操作", operationLog.getOperation());
        assertFalse(operationLog.getSuccess());
        assertEquals("RuntimeException", operationLog.getErrorType());
        assertEquals("boom", operationLog.getErrorMessage());
        assertNull(operationLog.getResponsePayload());
    }

    @Test
    void shouldRespectLogRequestAndLogResponseFlags() throws Throwable {
        OperationLogService operationLogService = mock(OperationLogService.class);
        AuditLogAspect aspect = aspect(operationLogService);
        ProceedingJoinPoint joinPoint = joinPoint("withoutPayload", "ok");

        Object result = aspect.audit(joinPoint);

        assertEquals("ok", result);
        OperationLog operationLog = captureOperationLog(operationLogService);
        assertTrue(operationLog.getSuccess());
        assertNull(operationLog.getRequestPayload());
        assertNull(operationLog.getResponsePayload());
    }

    @Test
    void shouldNotChangeBusinessResultWhenOperationLogServiceFails() throws Throwable {
        OperationLogService operationLogService = mock(OperationLogService.class);
        doThrow(new RuntimeException("db down")).when(operationLogService).saveSafely(any(OperationLog.class));
        AuditLogAspect aspect = aspect(operationLogService);
        ProceedingJoinPoint joinPoint = joinPoint("success", "ok");

        Object result = aspect.audit(joinPoint);

        assertEquals("ok", result);
    }

    @Test
    void shouldOnlyWriteLoggerWhenWriteDbDisabledOnSuccess() throws Throwable {
        OperationLogService operationLogService = mock(OperationLogService.class);
        AuditLogAspect aspect = aspect(operationLogService);
        ProceedingJoinPoint joinPoint = joinPoint("queryOnly", "ok");

        Object result = aspect.audit(joinPoint);

        assertEquals("ok", result);
        assertEquals(2, listAppender.list.size());
        assertEquals("AUDIT_API_REQUEST", listAppender.list.get(0).getFormattedMessage());
        assertEquals("AUDIT_API_RESPONSE", listAppender.list.get(1).getFormattedMessage());
        verify(operationLogService, never()).saveSafely(any(OperationLog.class));
    }

    @Test
    void shouldOnlyWriteLoggerWhenWriteDbDisabledOnError() throws Throwable {
        OperationLogService operationLogService = mock(OperationLogService.class);
        AuditLogAspect aspect = aspect(operationLogService);
        RuntimeException failure = new RuntimeException("boom");
        ProceedingJoinPoint joinPoint = joinPoint("queryOnlyFailure", failure);

        assertThrows(RuntimeException.class, () -> aspect.audit(joinPoint));

        assertEquals(2, listAppender.list.size());
        assertEquals("AUDIT_API_REQUEST", listAppender.list.get(0).getFormattedMessage());
        assertEquals("AUDIT_API_ERROR", listAppender.list.get(1).getFormattedMessage());
        verify(operationLogService, never()).saveSafely(any(OperationLog.class));
    }

    private AuditLogAspect aspect(OperationLogService operationLogService) {
        LoggingProperties properties = new LoggingProperties();
        properties.setAuditEnabled(true);
        return new AuditLogAspect(properties, new AuditPayloadSerializer(new ObjectMapper()), operationLogService);
    }

    private ProceedingJoinPoint joinPoint(String methodName, Object resultOrThrowable) throws Throwable {
        Method method = SampleController.class.getDeclaredMethod(methodName, String.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[] { "name" });

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new SampleController());
        when(joinPoint.getArgs()).thenReturn(new Object[] { "alice" });
        if (resultOrThrowable instanceof Throwable throwable) {
            when(joinPoint.proceed()).thenThrow(throwable);
        } else {
            when(joinPoint.proceed()).thenReturn(resultOrThrowable);
        }
        return joinPoint;
    }

    private OperationLog captureOperationLog(OperationLogService operationLogService) {
        org.mockito.ArgumentCaptor<OperationLog> captor = org.mockito.ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogService).saveSafely(captor.capture());
        return captor.getValue();
    }

    private void assertStructuredArgument(ILoggingEvent event, String key, String value) {
        assertTrue(Arrays.stream(event.getArgumentArray())
                .map(String::valueOf)
                .anyMatch(argument -> argument.contains(key) && argument.contains(value)));
    }

    static class SampleController {

        @AuditLog(operation = "成功操作")
        String success(String name) {
            return name;
        }

        @AuditLog(operation = "失败操作")
        String failure(String name) {
            return name;
        }

        @AuditLog(operation = "无payload操作", logRequest = false, logResponse = false)
        String withoutPayload(String name) {
            return name;
        }

        @AuditLog(operation = "查询操作", writeDb = false)
        String queryOnly(String name) {
            return name;
        }

        @AuditLog(operation = "查询失败操作", writeDb = false)
        String queryOnlyFailure(String name) {
            return name;
        }
    }
}
