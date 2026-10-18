package com.scaffold.mes.exec.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Batch 组批
 * <p>
 * 对应 mes_batch。OPEN 表示还在加人。达到 1 批后变为 READY。满员后不能再加。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_batch")
public class Batch extends NonSoftDeletableEntity {

    /** 批次号。 */
    private String batchNo;

    /** 站点。 */
    private Long stepId;

    /** 进站设备。 */
    private Long equipmentId;

    /** 本批 Recipe 版本。第一个带版本的 Lot 定下，之后必须相同。 */
    private Long recipeVersionId;

    /** 进站时间。 */
    private java.time.LocalDateTime trackInAt;

    /** 出站时间。 */
    private java.time.LocalDateTime trackOutAt;

    /** OPEN、READY、RUNNING、COMPLETED 或 CANCELLED。 */
    private String status;

    /** 最少 Lot 数。未达到时不能进站。 */
    private Integer minLots;

    /** 最多 Lot 数。 */
    private Integer maxLots;

    /** 当前 Lot 数。 */
    private Integer lotCount;

    /** 当前 Wafer 数。 */
    private Integer waferCount;

    /** 总数量。 */
    private Long totalQty;

    /** 乐观锁版本号。 */
    @Version
    private Integer version;
}
