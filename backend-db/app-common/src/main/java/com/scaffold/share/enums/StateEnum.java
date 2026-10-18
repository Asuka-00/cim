package com.scaffold.share.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;

import lombok.Getter;

/**
 * StateEnum枚举
 * <p>
 * 负责定义StateEnum相关常量与状态取值
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Getter
public enum StateEnum implements IEnum<Integer> {

    /**
     * 禁用
     */
    DISABLED(0, "禁用"),

    /**
     * 启用
     */
    ENABLED(1, "启用");

    @EnumValue
    private final Integer value;
    private final String description;

    StateEnum(Integer value, String description) {
        this.value = value;
        this.description = description;
    }
}
