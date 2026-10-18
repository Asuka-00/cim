package com.scaffold.share.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;

import lombok.Getter;

/**
 * VisibleEnum枚举
 * <p>
 * 负责定义VisibleEnum相关常量与状态取值
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Getter
public enum VisibleEnum implements IEnum<Integer> {

    /**
     * 隐藏
     */
    HIDDEN(0, "隐藏"),

    /**
     * 显示
     */
    VISIBLE(1, "显示");

    @EnumValue
    private final Integer value;
    private final String description;

    VisibleEnum(Integer value, String description) {
        this.value = value;
        this.description = description;
    }
}
