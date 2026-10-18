package com.scaffold.share.audit;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * OperationLogVO 操作日志展示对象
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-08
 */
@Data
public class OperationLogVO {

    private Long id;

    private String tenantId;

    private String traceId;

    private String userName;

    private String operation;

    private String className;

    private String methodName;

    private String requestUri;

    private String requestMethod;

    private String requestIp;

    private String userAgent;

    private String requestPayload;

    private String responsePayload;

    private Boolean success;

    private Long durationMs;

    private String errorType;

    private String errorMessage;

    private String createBy;

    private LocalDateTime createAt;
}
