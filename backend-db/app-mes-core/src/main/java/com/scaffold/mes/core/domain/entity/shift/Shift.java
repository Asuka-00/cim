package com.scaffold.mes.core.domain.entity.shift;

import java.time.LocalTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Shift 班次
 * <p>
 * 对应 mes_shift。结束时间不晚于开始时间时视为跨日。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_shift")
public class Shift extends NonSoftDeletableEntity {

    /** 班次编码。 */
    private String shiftCode;

    /** 班次名称。 */
    private String shiftName;

    /** 开始时间。 */
    private LocalTime startTime;

    /** 结束时间。 */
    private LocalTime endTime;

    /** 1 表示跨日。 */
    private Integer crossDay;
}
