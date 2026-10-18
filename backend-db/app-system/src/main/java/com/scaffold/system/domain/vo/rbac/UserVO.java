package com.scaffold.system.domain.vo.rbac;

import com.scaffold.share.enums.StateEnum;
import com.scaffold.share.vo.BaseVO;

import lombok.Getter;
import lombok.Setter;

/**
 * UserVO视图对象
 * <p>
 * 用户主数据列表/精简返回（不含角色子项）
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-16
 */
@Getter
@Setter
public class UserVO extends BaseVO {

    private String userName;

    private String displayName;

    private StateEnum enabled;

    private Boolean platformAdmin;

    /**
     * 所属租户名称，逗号拼接
     */
    private String tenantNames;
}
