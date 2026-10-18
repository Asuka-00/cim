package com.scaffold.mes.exec.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * WaferMap Wafer Map
 * <p>
 * 对应 mes_wafer_map。同一片可以有多个版本，只有一个当前版本。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_wafer_map")
public class WaferMap extends NonSoftDeletableEntity {

    /** Wafer。 */
    private Long waferId;

    /** 版本，重测递增。 */
    private Integer mapVersion;

    /** 1 表示当前版本。 */
    private Integer isCurrent;

    /** Lot。 */
    private Long lotId;

    /** 格式，首期为 CSV。 */
    private String mapFormat;

    /** 源文件名。 */
    private String fileName;

    /** Die 总数。 */
    private Integer totalDie;

    /** 良品 Die 数。 */
    private Integer goodDie;

    /** 不良 Die 数。 */
    private Integer badDie;

    /** 原始文本。 */
    private String mapData;

    /** 导入人。 */
    private String importedBy;

    /** 导入时间。 */
    private LocalDateTime importedAt;
}
