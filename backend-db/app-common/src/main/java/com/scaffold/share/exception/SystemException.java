package com.scaffold.share.exception;

/**
 * SystemException异常类
 * <p>
 * 负责封装SystemException相关异常语义与错误信息
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
public class SystemException extends RuntimeException {

    private final int code;
    private final String detail;
    private final String requestUri;

    public SystemException(int code, String message) {
        super(message);
        this.code = code;
        this.detail = null;
        this.requestUri = null;
    }

    public SystemException(int code, String message, String detail) {
        super(message);
        this.code = code;
        this.detail = detail;
        this.requestUri = null;
    }

    public SystemException(int code, String message, String detail, String requestUri) {
        super(message);
        this.code = code;
        this.detail = detail;
        this.requestUri = requestUri;
    }

    public SystemException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.detail = null;
        this.requestUri = null;
    }

    /**
     * 获取编码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取详情
     */
    public String getDetail() {
        return detail;
    }

    /**
     * 获取请求 URI
     */
    public String getRequestUri() {
        return requestUri;
    }
}
