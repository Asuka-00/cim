package com.scaffold.mes.base.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * ProductVersion 产品版本
 * <p>
 * 对应 mes_product_version。绑定的是 Route 和 BOM 主数据，不是某一个版本。下工单时再取当时的 ACTIVE 版本。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_product_version")
public class ProductVersion extends NonSoftDeletableEntity {

    /** 所属产品。 */
    private Long productId;

    /** 版本号，从 1 递增。 */
    private Integer versionNo;

    /** 状态。 */
    private String status;

    /** 绑定的 Route 主数据。 */
    private Long routeId;

    /** 绑定的 BOM 主数据。 */
    private Long bomId;

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
