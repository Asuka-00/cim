package com.scaffold.share.aop;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.lang.reflect.Method;
import java.util.Objects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.scaffold.share.config.LoggingProperties;
import com.scaffold.share.audit.OperationLogService;
import com.scaffold.share.audit.OperationLog;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.logging.AuditPayloadSerializer;
import com.scaffold.share.logging.MdcUtils;

import lombok.RequiredArgsConstructor;

/**
 * AuditLogAspect切面类
 * <p>
 * 负责记录Controller接口入参、出参和异常审计日志。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-23
 */
@Aspect
@RequiredArgsConstructor
@ConditionalOnProperty(name = "logging.audit-enabled", havingValue = "true", matchIfMissing = true)
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);
    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT");
    private static final String EVENT_REQUEST = "REQUEST";
    private static final String EVENT_RESPONSE = "RESPONSE";
    private static final String EVENT_ERROR = "ERROR";
    private static final int MAX_ERROR_MESSAGE_LENGTH = 1024;

    private final LoggingProperties loggingProperties;
    private final AuditPayloadSerializer payloadSerializer;
    private final OperationLogService operationLogService;

    /** Pointcut: 所有标注了@AuditLog注解的方法 */
    @Pointcut("@annotation(com.scaffold.share.logging.AuditLog)")
    public void auditLogMethods() {
    }

    /**
     * 环绕通知 - 记录接口审计日志
     */
    @Around("auditLogMethods()")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!loggingProperties.isAuditEnabled()) {
            return joinPoint.proceed();
        }

        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = Objects.requireNonNull(signature.getMethod());
        AuditLog auditLog = method.getAnnotation(AuditLog.class);

        String operation = resolveOperation(auditLog, method);
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();

        String requestPayload = null;
        if (auditLog.logRequest()) {
            requestPayload = payloadSerializer.serializeArguments(signature.getParameterNames(), joinPoint.getArgs(),
                    auditLog.mask(), auditLog.maxPayloadLength());
            logRequest(operation, className, methodName, requestPayload);
        }

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            String responsePayload = null;
            if (auditLog.logResponse()) {
                responsePayload = payloadSerializer.serializeValue(result, auditLog.mask(),
                        auditLog.maxPayloadLength());
                logResponse(operation, className, methodName, duration, responsePayload);
            }
            if (auditLog.writeDb()) {
                saveOperationLog(operation, className, methodName, requestPayload, responsePayload, duration, true,
                        null);
            }
            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;
            logError(operation, className, methodName, duration, ex);
            if (auditLog.writeDb()) {
                saveOperationLog(operation, className, methodName, requestPayload, null, duration, false, ex);
            }
            throw ex;
        }
    }

    private String resolveOperation(AuditLog auditLog, Method method) {
        if (auditLog.operation() != null && !auditLog.operation().isBlank()) {
            return auditLog.operation();
        }
        return method.getName();
    }

    private void logRequest(String operation, String className, String methodName, String payload) {
        AUDIT_LOG.info("AUDIT_API_REQUEST",
                kv("event", EVENT_REQUEST),
                kv("operation", operation),
                kv("className", className),
                kv("methodName", methodName),
                kv("userName", MdcUtils.getUserName()),
                kv("success", true),
                kv("payload", payload));
    }

    private void logResponse(String operation, String className, String methodName, long duration, String payload) {
        AUDIT_LOG.info("AUDIT_API_RESPONSE",
                kv("event", EVENT_RESPONSE),
                kv("operation", operation),
                kv("className", className),
                kv("methodName", methodName),
                kv("userName", MdcUtils.getUserName()),
                kv("duration", duration),
                kv("success", true),
                kv("payload", payload));
    }

    private void logError(String operation, String className, String methodName, long duration, Throwable ex) {
        AUDIT_LOG.warn("AUDIT_API_ERROR",
                kv("event", EVENT_ERROR),
                kv("operation", operation),
                kv("className", className),
                kv("methodName", methodName),
                kv("userName", MdcUtils.getUserName()),
                kv("duration", duration),
                kv("success", false),
                kv("errorType", ex.getClass().getSimpleName()),
                kv("errorMessage", ex.getMessage()));
    }

    private void saveOperationLog(String operation, String className, String methodName, String requestPayload,
            String responsePayload, long duration, boolean success, Throwable ex) {
        OperationLog operationLog = new OperationLog();
        operationLog.setTenantId(MdcUtils.getTenantId());
        operationLog.setTraceId(MdcUtils.getOrCreateTraceId());
        operationLog.setUserName(MdcUtils.getUserName());
        operationLog.setOperation(operation);
        operationLog.setClassName(className);
        operationLog.setMethodName(methodName);
        operationLog.setRequestUri(MdcUtils.getRequestUri());
        operationLog.setRequestMethod(MdcUtils.getRequestMethod());
        operationLog.setRequestIp(MdcUtils.getRequestIp());
        operationLog.setUserAgent(MdcUtils.getUserAgent());
        operationLog.setRequestPayload(requestPayload);
        operationLog.setResponsePayload(responsePayload);
        operationLog.setSuccess(success);
        operationLog.setDurationMs(duration);
        if (ex != null) {
            operationLog.setErrorType(ex.getClass().getSimpleName());
            operationLog.setErrorMessage(truncate(ex.getMessage(), MAX_ERROR_MESSAGE_LENGTH));
        }
        try {
            operationLogService.saveSafely(operationLog);
        } catch (RuntimeException saveEx) {
            log.warn("Failed to save operation log, traceId={}, operation={}",
                    operationLog.getTraceId(), operationLog.getOperation(), saveEx);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
