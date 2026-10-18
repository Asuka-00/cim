package com.scaffold.mes.core.domain.entity.calendar;

import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * FactoryCalendar 工厂日历
 * <p>
 * 对应 mes_calendar。同一天只能有一行。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_calendar")
public class FactoryCalendar extends NonSoftDeletableEntity {

    /** 日期。 */
    private LocalDate calDate;

    /** 1 表示工作日，0 表示休息日。 */
    private Integer isWorkday;

    /** 当日班次编码，逗号分隔。 */
    private String shiftCodes;
}
