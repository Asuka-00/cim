package com.scaffold.system.domain.dto.rbac;

import lombok.Data;

/**
 * 系统配置表中单条 OAuth2 厂商 JSON 结构
 * <p>
 * 仅描述如何连接 IdP 做身份认证；业务 Token 仍由本系统本地签发。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-07-17
 */
@Data
public class OAuth2ProviderConfig {

    /**
     * Spring registrationId，与 configCode 后缀 oauth2.provider.{id} 对齐
     */
    private String registrationId;

    /**
     * 登录页展示名
     */
    private String clientName;

    /**
     * 是否启用该厂商
     */
    private boolean enabled = true;

    private String clientId;

    private String clientSecret;

    /**
     * 逗号分隔 scope，如 openid,profile
     */
    private String scope = "openid,profile";

    /**
     * client_secret_basic / client_secret_post / none 等
     */
    private String clientAuthenticationMethod = "client_secret_basic";

    /**
     * 默认 authorization_code
     */
    private String authorizationGrantType = "authorization_code";

    /**
     * 默认 {baseUrl}/login/oauth2/code/{registrationId}
     */
    private String redirectUri = "{baseUrl}/login/oauth2/code/{registrationId}";

    /**
     * OIDC issuer；有值时走 Discovery
     */
    private String issuerUri;

    private String authorizationUri;

    private String tokenUri;

    private String userInfoUri;

    private String jwkSetUri;

    /**
     * 本厂商用户名 claim，覆盖全局 oauth2.user-name-claim
     */
    private String userNameAttribute;
}
