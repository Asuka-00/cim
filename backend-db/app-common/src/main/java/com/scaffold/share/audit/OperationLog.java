package com.scaffold.share.audit;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

/**
 * OperationLog 操作日志实体
 * <p>
 * 记录标注 {@code @AuditLog} 的接口调用审计信息，仅直写 MySQL。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-08
 */
@Getter
@Setter
@TableName("hist_operation_log")
public class OperationLog {

    /**
     * 主键ID（雪花算法）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /**
     * 链路追踪ID
     */
    private String traceId;

    /**
     * 用户代码
     */
    private String userName;

    /**
     * 操作描述
     */
    private String operation;

    /**
     * Controller类名
     */
    private String className;

    /**
     * Controller方法名
     */
    private String methodName;

    /**
     * 请求URI
     */
    private String requestUri;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求IP
     */
    private String requestIp;

    /**
     * User-Agent
     */
    private String userAgent;

    /**
     * 请求payload
     */
    private String requestPayload;

    /**
     * 响应payload
     */
    private String responsePayload;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 执行耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 异常类型
     */
    private String errorType;

    /**
     * 异常消息
     */
    private String errorMessage;

    /**
     * 创建人员
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createAt;
}
