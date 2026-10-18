package com.scaffold.system.domain.vo.rbac;

import com.scaffold.share.enums.StateEnum;
import com.scaffold.share.vo.BaseVO;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * RoleVO视图对象
 * <p>
 * 负责承载RoleVO相关展示与返回数据
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class RoleVO extends BaseVO {

    private String roleCode;

    private String roleName;

    private StateEnum enabled;
}
