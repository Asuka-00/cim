package com.scaffold.system.domain.repository.rbac;

import java.util.List;

import com.scaffold.system.domain.dto.rbac.OAuth2ProviderConfig;
import com.scaffold.system.domain.service.rbac.OAuth2Settings;

/**
 * OAuth2RegistrationRepository OAuth2 注册信息读取接口
 * <p>
 * domain 层只关心配置读取与热刷新，不涉及 Spring Security ClientRegistration 等基础设施类型。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-07-23
 */
public interface OAuth2RegistrationRepository {

    OAuth2Settings currentSettings();

    List<OAuth2ProviderConfig> listEnabledProviderConfigs();

    boolean isConfiguredRegistration(String registrationId);

    void reload();

    /**
     * 校验 registration 可用（连 IdP 验证），domain 只需知道是否成功，不需要 ClientRegistration 对象
     */
    void validateRegistration(String registrationId);
}
