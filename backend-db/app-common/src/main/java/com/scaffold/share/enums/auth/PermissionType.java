package com.scaffold.share.enums.auth;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;

import lombok.Getter;

/**
 * PermissionType枚举
 * <p>
 * 负责定义PermissionType相关常量与状态取值
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Getter
public enum PermissionType implements IEnum<Integer> {

    /**
     * 菜单
     */
    MENU(1, "菜单"),

    /**
     * 按钮
     */
    BUTTON(2, "按钮"),

    /**
     * 接口
     */
    API(3, "接口");

    @EnumValue
    private final Integer value;
    private final String description;

    PermissionType(Integer value, String description) {
        this.value = value;
        this.description = description;
    }
}
