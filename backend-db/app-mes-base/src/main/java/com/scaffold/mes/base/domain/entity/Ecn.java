package com.scaffold.mes.base.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Ecn 变更申请
 * <p>
 * 对应 mes_ecn。新建时状态固定为 DRAFT。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_ecn")
public class Ecn extends NonSoftDeletableEntity {

    /** 编号。 */
    private String ecnNo;

    /** 标题。 */
    private String title;

    /** ROUTE、RECIPE、BOM、PRODUCT 或 OTHER。 */
    private String changeType;

    /** 变更原因。 */
    private String reason;

    /** DRAFT。 */
    private String status;

    /** 申请人。 */
    private String applicant;

    /** 审批人。 */
    private String approver;

    /** 生效时间。 */
    private LocalDateTime effectiveAt;
}
