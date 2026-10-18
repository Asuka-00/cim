package com.scaffold.share.audit;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

/**
 * OperationLogQuery 操作日志查询条件
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-08
 */
@Data
public class OperationLogQuery {

    private String traceId;

    private String userName;

    private String operation;

    private String className;

    private String methodName;

    private String requestUri;

    private String requestMethod;

    private Boolean success;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createAtStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createAtEnd;
}
