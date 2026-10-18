package com.scaffold.share.exception;

import java.util.Map;

/**
 * BusinessException异常类
 * <p>
 * 负责封装BusinessException相关异常语义与错误信息
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
public class BusinessException extends RuntimeException {

    private final int code;
    private final Map<String, String> errorParams;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.errorParams = null;
    }

    public BusinessException(int code, String message, Map<String, String> errorParams) {
        super(message);
        this.code = code;
        this.errorParams = errorParams;
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.errorParams = null;
    }

    public BusinessException(int code, String message, Throwable cause, Map<String, String> errorParams) {
        super(message, cause);
        this.code = code;
        this.errorParams = errorParams;
    }

    /**
     * 获取编码
     */
    public int getCode() {
        return code;
    }

    public Map<String, String> getErrorParams() {
        return errorParams;
    }
}
