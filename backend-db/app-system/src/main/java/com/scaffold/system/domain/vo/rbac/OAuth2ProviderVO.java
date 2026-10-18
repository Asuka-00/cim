package com.scaffold.system.domain.vo.rbac;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 已启用的 OAuth2 身份提供方
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-07-17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2ProviderVO {

    /**
     * Spring registrationId，用于拼授权入口
     */
    private String registrationId;

    /**
     * 登录页展示名
     */
    private String displayName;
}
