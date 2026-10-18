package com.scaffold.mes.exec.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * CarrierBinding 载具绑定时间段
 * <p>
 * 对应 hist_mes_carrier_binding。绑定插入一行，解绑只回填结束时间。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("hist_mes_carrier_binding")
public class CarrierBinding extends NonSoftDeletableEntity {

    /** 载具。 */
    private Long carrierId;

    /** Lot。 */
    private Long lotId;

    /** 绑定时间。 */
    private LocalDateTime boundAt;

    /** 绑定人。 */
    private String boundBy;

    /** 解绑时间。空表示仍绑定。 */
    private LocalDateTime unboundAt;

    /** 解绑人。 */
    private String unboundBy;
}
