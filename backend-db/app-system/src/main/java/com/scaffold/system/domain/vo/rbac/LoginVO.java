package com.scaffold.system.domain.vo.rbac;

import java.util.List;

import com.scaffold.system.domain.vo.system.TenantSelectVO;

import lombok.Data;

/**
 * LoginVO视图对象
 * <p>
 * 负责承载LoginVO相关展示与返回数据
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
public class LoginVO {

    private String accessToken;

    private String refreshToken;

    private Long accessExpireSeconds;

    private String userName;

    private String displayName;

    private String tenantId;

    private Boolean platformAdmin;

    private List<TenantSelectVO> tenants;
}
