package com.scaffold.mes.exec.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.AppendOnlyEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * GenealogyLink 追溯关系
 * <p>
 * 对应 hist_mes_genealogy_link。同一对象对、关系类型和站点只记一条。数量以首次写入为准。txn_id 只在原事务已撤销时改指向新事务。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("hist_mes_genealogy_link")
public class GenealogyLink extends AppendOnlyEntity {

    /** 上游类型。 */
    private String parentType;

    /** 上游对象。 */
    private Long parentId;

    /** 上游编码。 */
    private String parentCode;

    /** 下游类型。 */
    private String childType;

    /** 下游对象。 */
    private Long childId;

    /** 下游编码。 */
    private String childCode;

    /** 关系类型。 */
    private String linkType;

    /** 发生站点。0 表示与站点无关。 */
    private Long stepId;

    /** 首次记录时的数量，不累计。 */
    private BigDecimal qty;

    /** 单位编码。 */
    private String unitCode;

    /** 发生设备。 */
    private Long equipmentId;

    /** 首次建立该关系的事务。撤销后再次发生时改指向新事务。 */
    private Long txnId;

    /** 操作人。 */
    private String operator;

    /** 发生时间。 */
    private LocalDateTime occurredAt;
}
