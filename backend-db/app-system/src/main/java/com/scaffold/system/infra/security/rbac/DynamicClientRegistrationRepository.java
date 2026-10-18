package com.scaffold.system.infra.security.rbac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.system.domain.dto.rbac.OAuth2ProviderConfig;
import com.scaffold.system.domain.repository.rbac.OAuth2RegistrationRepository;
import com.scaffold.system.domain.service.rbac.OAuth2Settings;
import com.scaffold.system.domain.service.rbac.OAuth2SettingsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于系统配置的可热刷新 {@link ClientRegistrationRepository}
 * <p>
 * 登录页列表只读配置（不连 IdP）；真正跳转授权时再懒加载 ClientRegistration（OIDC Discovery / 校验 IdP）。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-07-17
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DynamicClientRegistrationRepository
        implements ClientRegistrationRepository, OAuth2RegistrationRepository, Iterable<ClientRegistration> {

    private final OAuth2SettingsService oauth2SettingsService;
    private final OAuth2ClientRegistrationFactory registrationFactory;

    private final AtomicReference<OAuth2Settings> settingsRef = new AtomicReference<>(OAuth2Settings.defaults());
    /** registrationId -> 已启用的厂商配置（reload 时刷新） */
    private final ConcurrentHashMap<String, OAuth2ProviderConfig> enabledProviders = new ConcurrentHashMap<>();
    /** registrationId -> 懒构建的 ClientRegistration 缓存 */
    private final ConcurrentHashMap<String, ClientRegistration> registrations = new ConcurrentHashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        reload();
    }

    /**
     * 从系统配置重新加载设置；清空 ClientRegistration 缓存（下次点击登录再连 IdP）
     */
    public synchronized void reload() {
        OAuth2Settings settings = oauth2SettingsService.load();
        Map<String, OAuth2ProviderConfig> nextProviders = new ConcurrentHashMap<>();
        if (settings.isEnabled()) {
            for (OAuth2ProviderConfig provider : settings.getProviders()) {
                if (provider == null || !provider.isEnabled()) {
                    continue;
                }
                if (provider.getRegistrationId() == null || provider.getRegistrationId().isBlank()) {
                    continue;
                }
                nextProviders.put(provider.getRegistrationId().trim(), provider);
            }
        }
        enabledProviders.clear();
        enabledProviders.putAll(nextProviders);
        registrations.clear();
        settingsRef.set(settings);
        log.info("OAuth2 settings reloaded: enabled={}, configuredProviders={} (ClientRegistration lazy on login)",
                settings.isEnabled(), nextProviders.keySet());
    }

    /**
     * 当前缓存的全局设置（随 reload 更新）
     */
    public OAuth2Settings currentSettings() {
        return settingsRef.get();
    }

    /**
     * 已配置且 enabled 的厂商（供登录页展示，不访问 IdP）
     */
    public List<OAuth2ProviderConfig> listEnabledProviderConfigs() {
        return Collections.unmodifiableList(new ArrayList<>(enabledProviders.values()));
    }

    /**
     * 配置中是否存在已启用的 registrationId（不连 IdP）
     */
    public boolean isConfiguredRegistration(String registrationId) {
        if (registrationId == null || registrationId.isBlank()) {
            return false;
        }
        return enabledProviders.containsKey(registrationId.trim());
    }

    /**
     * 解析并构建 ClientRegistration：优先缓存，否则按配置连 IdP 构建。
     * 用于 authorize 点击与 Spring OAuth2 回调。
     */
    public ClientRegistration resolveRequired(String registrationId) {
        if (registrationId == null || registrationId.isBlank()) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "registrationId 不能为空");
        }
        String id = registrationId.trim();
        ClientRegistration cached = registrations.get(id);
        if (cached != null) {
            return cached;
        }
        OAuth2ProviderConfig config = enabledProviders.get(id);
        if (config == null) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "未知的 OAuth2 registrationId: " + id);
        }
        ClientRegistration built = registrationFactory.build(config);
        if (built == null) {
            throw new BusinessException(ErrorCodeConstants.OAUTH2_IDP_UNAVAILABLE,
                    "无法连接身份提供方，请检查 issuerUri/endpoint 配置: " + id);
        }
        ClientRegistration existing = registrations.putIfAbsent(id, built);
        return existing != null ? existing : built;
    }

    @Override
    public void validateRegistration(String registrationId) {
        resolveRequired(registrationId);
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        if (registrationId == null || registrationId.isBlank()) {
            return null;
        }
        try {
            return resolveRequired(registrationId);
        } catch (BusinessException ex) {
            log.warn("Resolve ClientRegistration failed: registrationId={}, message={}",
                    registrationId, ex.getMessage());
            return null;
        }
    }

    /**
     * 当前已构建成功的 registration（仅缓存中有值的）
     */
    public List<ClientRegistration> listRegistrations() {
        return Collections.unmodifiableList(new ArrayList<>(registrations.values()));
    }

    @Override
    public java.util.Iterator<ClientRegistration> iterator() {
        return listRegistrations().iterator();
    }
}
