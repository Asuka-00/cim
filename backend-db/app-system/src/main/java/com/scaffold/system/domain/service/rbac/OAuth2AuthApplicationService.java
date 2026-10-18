package com.scaffold.system.domain.service.rbac;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.system.domain.dto.rbac.OAuth2ProviderConfig;
import com.scaffold.system.domain.vo.rbac.LoginVO;
import com.scaffold.system.domain.vo.rbac.OAuth2ProviderVO;
import com.scaffold.system.domain.repository.rbac.OAuth2RegistrationRepository;

import lombok.RequiredArgsConstructor;

/**
 * OAuth2 Client 应用服务
 * <p>
 * 提供方列表与 ticket 换票；授权跳转由 Security Filter 完成。
 * 配置来自系统配置表，身份认证由 IdP 完成，业务 Token 由本系统签发。
 * <p>
 * 列表仅展示已配置且启用的厂商；点击登录时再连接 IdP 构建 ClientRegistration。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-07-17
 */
@Service
@RequiredArgsConstructor
public class OAuth2AuthApplicationService {

    private final OAuth2RegistrationRepository clientRegistrationRepository;
    private final OAuth2TicketService oauth2TicketService;

    /**
     * 已启用的 OAuth2 提供方（来自系统配置，不访问 IdP）
     */
    public List<OAuth2ProviderVO> listProviders() {
        OAuth2Settings settings = clientRegistrationRepository.currentSettings();
        if (!settings.isEnabled()) {
            return Collections.emptyList();
        }
        return clientRegistrationRepository.listEnabledProviderConfigs().stream()
                .map(this::toProviderVO)
                .toList();
    }

    /**
     * 校验 registration 已配置，并尝试连接 IdP（OIDC Discovery / 校验 endpoint）
     */
    public void requireEnabledRegistration(String registrationId) {
        OAuth2Settings settings = clientRegistrationRepository.currentSettings();
        if (!settings.isEnabled()) {
            throw new BusinessException(ErrorCodeConstants.OAUTH2_NOT_ENABLED, "OAuth2 登录未启用");
        }
        if (registrationId == null || registrationId.isBlank()) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "registrationId 不能为空");
        }
        if (!clientRegistrationRepository.isConfiguredRegistration(registrationId)) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM,
                    "未知的 OAuth2 registrationId: " + registrationId);
        }
        // 点击登录时再连 IdP；失败则直接业务错误，避免无意义 redirect
        clientRegistrationRepository.validateRegistration(registrationId);
    }

    /**
     * 消费 ticket 换取 LoginVO
     */
    public LoginVO exchangeTicket(String ticket) {
        OAuth2Settings settings = clientRegistrationRepository.currentSettings();
        if (!settings.isEnabled()) {
            throw new BusinessException(ErrorCodeConstants.OAUTH2_NOT_ENABLED, "OAuth2 登录未启用");
        }
        return oauth2TicketService.consumeTicket(ticket);
    }

    private OAuth2ProviderVO toProviderVO(OAuth2ProviderConfig config) {
        String name = config.getClientName();
        if (name == null || name.isBlank()) {
            name = config.getRegistrationId();
        }
        return new OAuth2ProviderVO(config.getRegistrationId(), name);
    }
}
