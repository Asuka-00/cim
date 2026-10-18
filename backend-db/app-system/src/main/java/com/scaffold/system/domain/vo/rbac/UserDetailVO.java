package com.scaffold.system.domain.vo.rbac;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * UserDetailVO视图对象
 * <p>
 * 用户主数据详情返回（继承列表字段 + 角色子项）
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-05-15
 */
@Getter
@Setter
public class UserDetailVO extends UserVO {

    /**
     * 各租户成员资格与角色
     */
    private List<UserTenantMembershipVO> memberships;
}
