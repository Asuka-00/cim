package com.scaffold.share.aop;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.scaffold.share.config.LoggingProperties;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.logging.LogConstants;
import com.scaffold.share.logging.MdcUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 性能日志切面：Service / Repository 耗时监控。
 * <p>
 * 慢调用与非业务异常以 WARN 写入 PERFORMANCE 通道（performance.log）；
 * message 使用稳定 event code，字段走 StructuredArguments，便于机器检索。
 *
 * @author Kairo
 * @version 1.1.0
 * @since 2026-04-09
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
@ConditionalOnProperty(name = "logging.performance-enabled", havingValue = "true", matchIfMissing = true)
public class PerformanceLogAspect {

    private static final Logger PERFORMANCE_LOG = LoggerFactory.getLogger("PERFORMANCE");

    private static final String EVENT_OK = "PERFORMANCE_OK";
    private static final String EVENT_SLOW = "PERFORMANCE_SLOW";
    private static final String EVENT_ERROR = "PERFORMANCE_ERROR";

    /** 慢调用阈值(毫秒) */
    private static final long SLOW_THRESHOLD_MS = 1000;

    private final LoggingProperties loggingProperties;

    @Pointcut("execution(* com.scaffold..service..*.*(..))")
    public void serviceMethods() {
    }

    @Pointcut("execution(* com.scaffold..repository..*.*(..))")
    public void repositoryMethods() {
    }

    @Around("serviceMethods() || repositoryMethods()")
    public Object logPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!loggingProperties.isPerformanceEnabled()) {
            return joinPoint.proceed();
        }

        long startNanos = System.nanoTime();
        boolean success = true;
        Throwable error = null;

        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            success = false;
            error = ex;
            throw ex;
        } finally {
            long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            // 快路径（成功且不慢、未开 debug）不碰 MDC / 反射取名：该切面包住所有 service/repository 调用
            if (shouldLog(success, error, duration)) {
                log(joinPoint, success, error, duration);
            }
        }
    }

    private boolean shouldLog(boolean success, Throwable error, long duration) {
        if (!success && !(error instanceof BusinessException)) {
            return true;
        }
        if (success && duration > SLOW_THRESHOLD_MS) {
            return true;
        }
        return PERFORMANCE_LOG.isDebugEnabled();
    }

    private void log(ProceedingJoinPoint joinPoint, boolean success, Throwable error, long duration) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String traceId = MdcUtils.getTraceId();

            String previousDuration = MdcUtils.getDuration();
            String previousSuccess = MdcUtils.getSuccess();
            String previousClassName = MDC.get(LogConstants.CLASS_NAME);
            String previousMethodName = MDC.get(LogConstants.METHOD_NAME);

            MdcUtils.setDuration(duration);
            MdcUtils.setSuccess(success);
            MdcUtils.put(LogConstants.CLASS_NAME, className);
            MdcUtils.put(LogConstants.METHOD_NAME, methodName);

            try {
                if (!success) {
                    if (error instanceof BusinessException) {
                        // 预期业务拒绝不污染 performance 文件
                        if (PERFORMANCE_LOG.isDebugEnabled()) {
                            PERFORMANCE_LOG.debug("PERFORMANCE_OK {}.{} {}ms business_reject",
                                    className, methodName, duration,
                                    kv("event", EVENT_OK),
                                    kv("className", className),
                                    kv("methodName", methodName),
                                    kv("duration", duration),
                                    kv("threshold", SLOW_THRESHOLD_MS),
                                    kv("success", false),
                                    kv("errorType", "BusinessException"),
                                    kv("traceId", traceId));
                        }
                    } else {
                        String errorType = error != null ? error.getClass().getSimpleName() : "none";
                        PERFORMANCE_LOG.warn("PERFORMANCE_ERROR {}.{} {}ms {}",
                                className, methodName, duration, errorType,
                                kv("event", EVENT_ERROR),
                                kv("className", className),
                                kv("methodName", methodName),
                                kv("duration", duration),
                                kv("threshold", SLOW_THRESHOLD_MS),
                                kv("success", false),
                                kv("errorType", errorType),
                                kv("traceId", traceId));
                    }
                } else if (duration > SLOW_THRESHOLD_MS) {
                    PERFORMANCE_LOG.warn("PERFORMANCE_SLOW {}.{} {}ms (threshold={}ms)",
                            className, methodName, duration, SLOW_THRESHOLD_MS,
                            kv("event", EVENT_SLOW),
                            kv("className", className),
                            kv("methodName", methodName),
                            kv("duration", duration),
                            kv("threshold", SLOW_THRESHOLD_MS),
                            kv("success", true),
                            kv("traceId", traceId));
                } else if (PERFORMANCE_LOG.isDebugEnabled()) {
                    PERFORMANCE_LOG.debug("PERFORMANCE_OK {}.{} {}ms",
                            className, methodName, duration,
                            kv("event", EVENT_OK),
                            kv("className", className),
                            kv("methodName", methodName),
                            kv("duration", duration),
                            kv("threshold", SLOW_THRESHOLD_MS),
                            kv("success", true),
                            kv("traceId", traceId));
                }
            } finally {
                restoreMdc(LogConstants.DURATION, previousDuration);
                restoreMdc(LogConstants.SUCCESS, previousSuccess);
                restoreMdc(LogConstants.CLASS_NAME, previousClassName);
                restoreMdc(LogConstants.METHOD_NAME, previousMethodName);
            }
    }

    private void restoreMdc(String key, String previousValue) {
        if (previousValue == null) {
            MdcUtils.remove(key);
            return;
        }
        MdcUtils.put(key, previousValue);
    }
}
