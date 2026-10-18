package com.scaffold.system.domain.query.rbac;

import com.scaffold.share.enums.StateEnum;

import lombok.Data;

/**
 * UserQuery类
 * <p>
 * 负责处理UserQuery相关核心业务能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
public class UserQuery {

    private String userName;

    private String displayName;

    private StateEnum enabled;
}
