package com.scaffold.share.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;

import lombok.Getter;

/**
 * IsAdminEnum枚举
 * <p>
 * 负责定义IsAdminEnum相关常量与状态取值
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Getter
public enum IsAdminEnum implements IEnum<Integer> {

    /**
     * 非管理员
     */
    NOT_ADMIN(0, "非管理员"),

    /**
     * 管理员
     */
    IS_ADMIN(1, "管理员"),

    /**
     * 超级管理员
     */
    SUPER_ADMIN(2, "超级管理员");

    @EnumValue
    private final Integer value;
    private final String description;

    IsAdminEnum(Integer value, String description) {
        this.value = value;
        this.description = description;
    }
}