package com.scaffold.mes.exec.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * LotStepPass Lot 在某站点已 Track-Out 的次数
 * <p>
 * 对应 mes_lot_step_pass。到达站点时 Lot 的 pass_no 等于 pass_count + 1。返工与主流程按同一 step_id 累加。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_lot_step_pass")
public class LotStepPass extends NonSoftDeletableEntity {

    /** Lot。 */
    private Long lotId;

    /** 站点。 */
    private Long stepId;

    /** 已 Track-Out 次数。 */
    private Integer passCount;
}
