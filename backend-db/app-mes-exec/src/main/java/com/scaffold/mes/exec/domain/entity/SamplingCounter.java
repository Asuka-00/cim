package com.scaffold.mes.exec.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * SamplingCounter 抽检计数器
 * <p>
 * 对应 mes_sampling_counter。每进一个 Lot 加 1，满 N 时抽中并清零。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_sampling_counter")
public class SamplingCounter extends NonSoftDeletableEntity {

    /** 规则。 */
    private Long ruleId;

    /** 站点。 */
    private Long stepId;

    /** Route 步骤。0 表示不区分步骤。 */
    private Long routeStepId;

    /** 产品。0 表示通用。 */
    private Long productId;

    /** 当前计数。 */
    private Integer counter;

    /** 最近被抽中的 Lot。 */
    private Long lastSampledLotId;

    /** 最近抽中时间。 */
    private LocalDateTime lastSampledAt;

    /** 乐观锁版本号。 */
    @Version
    private Integer version;
}
