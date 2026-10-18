package com.scaffold.mes.exec.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Handover 交接班
 * <p>
 * 对应 mes_handover。新建时状态固定为 DRAFT。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_handover")
public class Handover extends NonSoftDeletableEntity {

    /** 交接单号。 */
    private String handoverNo;

    /** 交班人。 */
    private String fromUser;

    /** 接班人。 */
    private String toUser;

    /** DRAFT、SUBMITTED 或 CONFIRMED。 */
    private String status;

    /** 交接快照，提交时写入。 */
    private String contentJson;

    /** 交班备注。 */
    private String remark;

    /** 提交时间。 */
    private LocalDateTime submittedAt;

    /** 确认时间。 */
    private LocalDateTime confirmedAt;
}
