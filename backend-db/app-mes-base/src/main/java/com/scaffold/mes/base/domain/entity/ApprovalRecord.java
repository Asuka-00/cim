package com.scaffold.mes.base.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.AppendOnlyEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * ApprovalRecord 版本审批记录
 * <p>
 * 对应 hist_mes_approval_record。只追加，不修改。Route、BOM、Recipe、产品版本共用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("hist_mes_approval_record")
public class ApprovalRecord extends AppendOnlyEntity {

    /** 业务类型。 */
    private String bizType;

    /** 业务对象主键。 */
    private Long bizId;

    /** 动作：SUBMIT、APPROVE、REJECT、REVOKE。 */
    private String action;

    /** 变更前状态。 */
    private String fromStatus;

    /** 变更后状态。 */
    private String toStatus;

    /** 指定审批人。 */
    private String assignee;

    /** 操作人。 */
    private String operator;

    /** 意见。 */
    private String comment;

    /** 动作时间。 */
    private LocalDateTime actionAt;
}
