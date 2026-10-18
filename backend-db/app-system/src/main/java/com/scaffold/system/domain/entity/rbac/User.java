package com.scaffold.system.domain.entity.rbac;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.SoftDeletableEntity;
import com.scaffold.share.enums.StateEnum;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * User 用户实体
 * <p>
 * 记录系统用户信息，包括账号、密码、部门和启用状态。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Getter
@Setter
@FieldNameConstants
@TableName("sys_user")
public class User extends SoftDeletableEntity {

    /**
     * 登录名
     */
    private String userName;

    /**
     * 显示名
     */
    private String displayName;

    /**
     * 加密密码
     */
    private String passwordHash;

    private String salt;

    private LocalDateTime lastLoginAt;

    /**
     * 状态（1:启用 0:禁用）
     */
    private StateEnum enabled;

    /**
     * 平台超管：可切任意租户并看到完整权限目录
     */
    private Boolean platformAdmin;

    /**
     * 初始化用户密码，入参必须是已加密密码。
     */
    public void initializePassword(String encodedPassword) {
        this.passwordHash = encodedPassword;
    }

    /**
     * 重置用户密码，入参必须是已加密密码。
     */
    public void resetPassword(String encodedDefaultPassword) {
        this.passwordHash = encodedDefaultPassword;
    }

    /**
     * 修改用户密码，入参必须是已加密密码。
     */
    public void changePassword(String encodedPassword) {
        this.passwordHash = encodedPassword;
    }
}
