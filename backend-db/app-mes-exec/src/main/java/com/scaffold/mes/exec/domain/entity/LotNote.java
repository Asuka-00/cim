package com.scaffold.mes.exec.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * LotNote Lot 备注
 * <p>
 * 对应 mes_lot_note。Hold 中也可以追加。置顶的备注在列表前面。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_lot_note")
public class LotNote extends NonSoftDeletableEntity {

    /** Lot。 */
    private Long lotId;

    /** NOTE、WARNING、ENGINEERING 或 TAG。 */
    private String noteType;

    /** 内容。 */
    private String content;

    /** 1 表示置顶。 */
    private Integer pinned;
}
