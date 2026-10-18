package com.scaffold.mes.exec.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * LotHold 一条未合并的 Hold
 * <p>
 * 对应 mes_lot_hold。Hold 是叠加标记，不是 Lot 状态。手工 Hold 的 active_key 为空，允许同一原因多次。系统 Hold 用 active_key 幂等。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_lot_hold")
public class LotHold extends NonSoftDeletableEntity {

    /** Lot。 */
    private Long lotId;

    /** 原因码。 */
    private String reasonCode;

    /** 类别，取自原因码。 */
    private String holdType;

    /** 来源。 */
    private String holdSource;

    /** ACTIVE 或 RELEASED。 */
    private String status;

    /** 系统 Hold 的幂等键。手工 Hold 与已释放为空。 */
    private String activeKey;

    /** Hold 发生站点。 */
    private Long holdStepId;

    /** Hold 发生步骤序号。 */
    private Integer holdSeqNo;

    /** Hold 操作人。 */
    private String holdBy;

    /** Hold 时间。 */
    private LocalDateTime holdAt;

    /** Hold 备注。 */
    private String holdRemark;

    /** 要求的 Release 角色。空表示不限。 */
    private String releaseRoleCode;

    /** Release 操作人。 */
    private String releaseBy;

    /** Release 时间。 */
    private LocalDateTime releaseAt;

    /** Release 备注。 */
    private String releaseRemark;

    /** 来源单据。0 表示没有。 */
    private Long sourceRefId;
}
