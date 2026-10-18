package com.scaffold.share.vo;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * Result类
 * <p>
 * 负责处理Result相关核心业务能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> errorParams;

    /**
     * 返回成功结果
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    /**
     * 返回成功结果
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 返回失败结果
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return fail(code, message, null);
    }

    /**
     * 返回失败结果
     */
    public static <T> Result<T> fail(Integer code, String message, Map<String, String> errorParams) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setErrorParams(errorParams);
        return result;
    }
}
