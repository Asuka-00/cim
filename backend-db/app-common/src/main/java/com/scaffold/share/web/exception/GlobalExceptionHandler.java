package com.scaffold.share.web.exception;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.exception.SystemException;
import com.scaffold.share.logging.MdcUtils;
import com.scaffold.share.vo.Result;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/**
 * 全局异常处理器。
 * <p>
 * 统一封装 Spring MVC、业务异常和系统异常的 API 错误响应。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger ERROR_LOG = LoggerFactory.getLogger("ERROR");

    // ==================== Spring MVC 异常处理 ====================

    /**
     * JSON解析失败 / 未知字段
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String detail = extractJsonErrorDetail(ex);
        String message = detail != null ? detail : "请求体格式错误或包含未知字段";

        logError(ErrorCodeConstants.INVALID_REQUEST, message, ex, request);
        return Result.fail(ErrorCodeConstants.INVALID_REQUEST, message);
    }

    /**
     * 缺少必需请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Object> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String detail = String.format("缺少必需参数: %s (类型: %s)", ex.getParameterName(), ex.getParameterType());

        logError(ErrorCodeConstants.MISSING_PARAM, detail, ex, request);
        return Result.fail(ErrorCodeConstants.MISSING_PARAM, detail);
    }

    /**
     * 参数类型转换失败
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        Class<?> requiredType = ex.getRequiredType();
        String detail = String.format("参数 %s 类型错误，期望: %s，实际: %s",
                ex.getName(),
                requiredType != null ? requiredType.getSimpleName() : "未知",
                ex.getValue());

        logError(ErrorCodeConstants.PARAM_TYPE_MISMATCH, detail, ex, request);
        return Result.fail(ErrorCodeConstants.PARAM_TYPE_MISMATCH, detail);
    }

    /**
     * 约束校验失败（路径变量上的校验等）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Object> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String detail = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));

        logError(ErrorCodeConstants.CONSTRAINT_VIOLATION, detail, ex, request);
        return Result.fail(ErrorCodeConstants.CONSTRAINT_VIOLATION, detail);
    }

    /**
     * 请求体参数校验失败（如 @NotBlank, @NotNull 等注解校验失败）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        logError(ErrorCodeConstants.PARAM_VALIDATION_ERROR, detail, ex, request);
        return Result.fail(ErrorCodeConstants.PARAM_VALIDATION_ERROR, detail);
    }

    /**
     * 请求绑定失败
     */
    @ExceptionHandler(ServletRequestBindingException.class)
    public Result<Object> handleBindingError(ServletRequestBindingException ex, HttpServletRequest request) {
        String detail = "请求绑定失败: " + ex.getMessage();

        logError(ErrorCodeConstants.BINDING_ERROR, detail, ex, request);
        return Result.fail(ErrorCodeConstants.BINDING_ERROR, detail);
    }

    /**
     * HTTP方法不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Object> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        String detail = String.format("不支持的HTTP方法: %s，支持: %s",
                ex.getMethod(),
                Arrays.toString(ex.getSupportedMethods()));

        logError(ErrorCodeConstants.METHOD_NOT_SUPPORTED, detail, ex, request);
        return Result.fail(ErrorCodeConstants.METHOD_NOT_SUPPORTED, detail);
    }

    // ==================== 业务异常处理 ====================

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Object> handleBusinessException(BusinessException ex) {
        ERROR_LOG.warn("BUSINESS_EXCEPTION - traceId: {}, code: {}, message: {}",
                getSafeTraceId(),
                ex.getCode(),
                ex.getMessage());
        return Result.fail(ex.getCode(), ex.getMessage(), ex.getErrorParams());
    }

    // ==================== 系统异常处理 ====================

    /**
     * 处理系统异常
     */
    @ExceptionHandler(SystemException.class)
    public Result<Object> handleSystemException(SystemException ex) {
        String detail = ex.getDetail() != null ? ex.getDetail() : "";
        String requestUri = ex.getRequestUri() != null ? ex.getRequestUri() : "unknown";

        ERROR_LOG.error("SYSTEM_EXCEPTION - traceId: {}, code: {}, message: {}, detail: {}, requestUri: {}",
                getSafeTraceId(),
                ex.getCode(),
                ex.getMessage(),
                detail,
                requestUri);
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    // ==================== 异步 / 客户端生命周期 ====================

    /**
     * SSE / DeferredResult 异步超时（如 SseEmitter 到期）。
     * <p>
     * 属连接生命周期，不是业务或系统故障，禁止落入 UNKNOWN_EXCEPTION。
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public Result<Object> handleAsyncRequestTimeout(AsyncRequestTimeoutException ex, HttpServletRequest request) {
        ERROR_LOG.debug("ASYNC_REQUEST_TIMEOUT - traceId: {}, userName: {}, requestUri: {}, message: {}",
                getSafeTraceId(),
                MdcUtils.getUserName() != null ? MdcUtils.getUserName() : "UNKNOWN",
                request != null ? request.getRequestURI() : "unknown",
                ex.getMessage());
        return null;
    }

    // ==================== 兜底异常处理 ====================

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception ex) {
        if (isClientDisconnected(ex)) {
            logClientDisconnected(ex);
            return null;
        }
        logFullStackTrace(ErrorCodeConstants.UNKNOWN_ERROR, "系统异常，请稍后重试", ex);
        return Result.fail(ErrorCodeConstants.UNKNOWN_ERROR, "系统异常，请稍后重试");
    }

    // ==================== 辅助方法 ====================

    /**
     * 记录错误日志
     */
    private void logError(int code, String message, Exception ex, HttpServletRequest request) {
        String traceId = getSafeTraceId();
        String userName = MdcUtils.getUserName();
        String requestUri = request != null ? request.getRequestURI() : "unknown";

        ERROR_LOG.error(
                "SYSTEM_EXCEPTION - traceId: {}, userName: {}, code: {}, message: {}, requestUri: {}, stackTrace: {}",
                traceId,
                userName != null ? userName : "UNKNOWN",
                code,
                message,
                requestUri,
                getStackTrace(ex));
    }

    /**
     * 记录完整堆栈日志
     */
    private void logFullStackTrace(int code, String message, Exception ex) {
        String traceId = getSafeTraceId();
        String userName = MdcUtils.getUserName();

        ERROR_LOG.error("UNKNOWN_EXCEPTION - traceId: {}, userName: {}, code: {}, message: {}, stackTrace: {}",
                traceId,
                userName != null ? userName : "UNKNOWN",
                code,
                message,
                getFullStackTrace(ex));
    }

    /**
     * 压测或网络波动下客户端可能先断开连接，此时响应流已不可写，不应记录为服务端系统异常。
     */
    private boolean isClientDisconnected(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof AsyncRequestNotUsableException) {
                return true;
            }
            String className = current.getClass().getName();
            String message = current.getMessage();
            if (className.contains("ClientAbortException")
                    || containsIgnoreCase(message, "Broken pipe")
                    || containsIgnoreCase(message, "Connection reset by peer")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword.toLowerCase());
    }

    private void logClientDisconnected(Exception ex) {
        ERROR_LOG.debug("CLIENT_DISCONNECTED - traceId: {}, userName: {}, exception: {}, message: {}",
                getSafeTraceId(),
                MdcUtils.getUserName() != null ? MdcUtils.getUserName() : "UNKNOWN",
                ex.getClass().getName(),
                ex.getMessage());
    }

    /**
     * 获取安全追踪标识
     */
    private String getSafeTraceId() {
        String traceId = MdcUtils.getTraceId();
        return traceId != null ? traceId : "NO_TRACE-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 获取堆栈信息
     */
    private String getStackTrace(Exception ex) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : ex.getStackTrace()) {
            if (element.toString().contains("com.scaffold")) {
                sb.append(element).append("\n");
                if (sb.length() > 500) {
                    sb.append("...(truncated)");
                    break;
                }
            }
        }
        return sb.length() > 0 ? sb.toString() : ex.getClass().getName();
    }

    /**
     * 获取完整堆栈信息
     */
    private String getFullStackTrace(Exception ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        String stackTrace = sw.toString();
        if (stackTrace.length() > 1000) {
            return stackTrace.substring(0, 1000) + "...(truncated)";
        }
        return stackTrace;
    }

    /**
     * 提取JSON error详情
     */
    private String extractJsonErrorDetail(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        if (cause == null) {
            return null;
        }

        if (cause instanceof UnrecognizedPropertyException upe) {
            return String.format("未知字段: %s", upe.getPropertyName());
        }

        if (cause instanceof JsonParseException) {
            return "JSON格式错误: " + cause.getMessage();
        }

        if (cause instanceof MismatchedInputException mie) {
            String path = mie.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(p -> p != null)
                    .collect(Collectors.joining("."));
            String type = mie.getTargetType() != null ? mie.getTargetType().getSimpleName() : "未知";
            return String.format("字段类型不匹配: %s，期望: %s", path, type);
        }

        return null;
    }
}
