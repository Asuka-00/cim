package com.scaffold.mes.base.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * RouteVersion Route 版本
 * <p>
 * 对应 mes_route_version。状态为 DRAFT、PENDING、ACTIVE、INACTIVE。生效后不可改。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_route_version")
public class RouteVersion extends NonSoftDeletableEntity {

    /** 所属 Route。 */
    private Long routeId;

    /** 版本号，从 1 递增。 */
    private Integer versionNo;

    /** 状态。 */
    private String status;

    /** 复制来源版本。 */
    private Long copyFromVersionId;

    /** 生效时间。 */
    private LocalDateTime effectiveAt;

    /** 失效时间。 */
    private LocalDateTime expireAt;

    /** 提交人。 */
    private String submittedBy;

    /** 最近审批人。 */
    private String approvedBy;

    /** 最近审批时间。 */
    private LocalDateTime approvedAt;

    /** 备注。 */
    private String remark;
}
