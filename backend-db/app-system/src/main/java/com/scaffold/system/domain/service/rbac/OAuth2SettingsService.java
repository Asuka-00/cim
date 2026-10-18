package com.scaffold.system.domain.service.rbac;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.share.enums.StateEnum;
import com.scaffold.system.domain.dto.rbac.OAuth2ProviderConfig;
import com.scaffold.system.domain.entity.system.SystemConfig;
import com.scaffold.system.domain.repository.system.SystemConfigRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 从 sys_system_config（group=OAUTH2）加载 OAuth2 设置
 * <p>
 * 登录前无租户上下文，使用 ignore-tenant 读库，保证配置更新后立即热刷新。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-07-17
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2SettingsService {

    public static final String GROUP_CODE = "OAUTH2";
    public static final String CODE_ENABLED = "oauth2.enabled";
    public static final String CODE_USER_NAME_CLAIM = "oauth2.user-name-claim";
    public static final String CODE_FRONTEND_SUCCESS_URL = "oauth2.frontend-success-url";
    public static final String CODE_FRONTEND_FAILURE_URL = "oauth2.frontend-failure-url";
    public static final String CODE_TICKET_TTL_SECONDS = "oauth2.ticket-ttl-seconds";
    public static final String PROVIDER_PREFIX = "oauth2.provider.";

    private final SystemConfigRepository systemConfigRepository;
    private final ObjectMapper objectMapper;

    /**
     * 加载最新 OAuth2 设置（无缓存；调用方可在 Repository 层缓存 ClientRegistration）
     */
    public OAuth2Settings load() {
        List<SystemConfig> rows;
        try {
            rows = systemConfigRepository.findAllByGroupCodeIgnoreTenant(GROUP_CODE);
        } catch (Exception ex) {
            log.warn("Load OAuth2 system configs failed, using defaults: {}", ex.getMessage());
            return OAuth2Settings.defaults();
        }
        if (rows == null || rows.isEmpty()) {
            return OAuth2Settings.defaults();
        }

        OAuth2Settings defaults = OAuth2Settings.defaults();
        boolean enabled = defaults.isEnabled();
        String userNameClaim = defaults.getUserNameClaim();
        String successUrl = defaults.getFrontendSuccessUrl();
        String failureUrl = defaults.getFrontendFailureUrl();
        long ticketTtl = defaults.getTicketTtlSeconds();
        List<OAuth2ProviderConfig> providers = new ArrayList<>();

        for (SystemConfig row : rows) {
            if (row == null || row.getConfigCode() == null) {
                continue;
            }
            if (row.getState() != null && row.getState() != StateEnum.ENABLED) {
                continue;
            }
            String code = row.getConfigCode().trim();
            String value = row.getConfigValue();
            if (CODE_ENABLED.equals(code)) {
                enabled = parseBoolean(value, false);
            } else if (CODE_USER_NAME_CLAIM.equals(code)) {
                if (hasText(value)) {
                    userNameClaim = value.trim();
                }
            } else if (CODE_FRONTEND_SUCCESS_URL.equals(code)) {
                if (hasText(value)) {
                    successUrl = value.trim();
                }
            } else if (CODE_FRONTEND_FAILURE_URL.equals(code)) {
                if (hasText(value)) {
                    failureUrl = value.trim();
                }
            } else if (CODE_TICKET_TTL_SECONDS.equals(code)) {
                ticketTtl = parseLong(value, defaults.getTicketTtlSeconds());
            } else if (code.startsWith(PROVIDER_PREFIX)) {
                OAuth2ProviderConfig provider = parseProvider(code, value);
                if (provider != null) {
                    providers.add(provider);
                }
            }
        }

        return OAuth2Settings.builder()
                .enabled(enabled)
                .userNameClaim(userNameClaim)
                .frontendSuccessUrl(successUrl)
                .frontendFailureUrl(failureUrl)
                .ticketTtlSeconds(Math.max(1, ticketTtl))
                .providers(List.copyOf(providers))
                .build();
    }

    /**
     * 是否属于 OAuth2 配置变更（用于热刷新判断）
     */
    public static boolean isOAuth2Config(String groupCode, String configCode) {
        if (GROUP_CODE.equalsIgnoreCase(groupCode)) {
            return true;
        }
        return configCode != null && configCode.startsWith("oauth2.");
    }

    private OAuth2ProviderConfig parseProvider(String configCode, String json) {
        if (!hasText(json)) {
            log.warn("OAuth2 provider config empty: {}", configCode);
            return null;
        }
        try {
            OAuth2ProviderConfig config = objectMapper.readValue(json, OAuth2ProviderConfig.class);
            String idFromCode = configCode.substring(PROVIDER_PREFIX.length()).trim();
            if (!hasText(config.getRegistrationId())) {
                config.setRegistrationId(idFromCode);
            } else if (!idFromCode.equals(config.getRegistrationId())) {
                log.warn("OAuth2 provider registrationId mismatch: configCode={}, jsonId={}, using configCode",
                        configCode, config.getRegistrationId());
                config.setRegistrationId(idFromCode);
            }
            if (!hasText(config.getClientId())) {
                log.warn("OAuth2 provider missing clientId: {}", configCode);
                return null;
            }
            if (!hasText(config.getClientName())) {
                config.setClientName(config.getRegistrationId());
            }
            return config;
        } catch (Exception ex) {
            log.warn("Parse OAuth2 provider JSON failed: code={}, error={}", configCode, ex.getMessage());
            return null;
        }
    }

    private static boolean parseBoolean(String value, boolean defaultValue) {
        if (!hasText(value)) {
            return defaultValue;
        }
        String v = value.trim();
        if ("1".equals(v) || "true".equalsIgnoreCase(v) || "yes".equalsIgnoreCase(v)) {
            return true;
        }
        if ("0".equals(v) || "false".equalsIgnoreCase(v) || "no".equalsIgnoreCase(v)) {
            return false;
        }
        return defaultValue;
    }

    private static long parseLong(String value, long defaultValue) {
        if (!hasText(value)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
