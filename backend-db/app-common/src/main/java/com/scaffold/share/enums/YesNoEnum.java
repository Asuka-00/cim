package com.scaffold.share.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;

import lombok.Getter;

/**
 * YesNoEnum枚举
 * <p>
 * 负责定义YesNoEnum相关常量与状态取值
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Getter
public enum YesNoEnum implements IEnum<Integer> {

    /**
     * 否
     */
    NO(0, "否"),

    /**
     * 是
     */
    YES(1, "是");

    @EnumValue
    private final Integer value;
    private final String description;

    YesNoEnum(Integer value, String description) {
        this.value = value;
        this.description = description;
    }
}
