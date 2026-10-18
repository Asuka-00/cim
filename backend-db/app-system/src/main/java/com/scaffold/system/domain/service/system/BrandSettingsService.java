package com.scaffold.system.domain.service.system;

import java.util.List;

import org.springframework.stereotype.Service;

import com.scaffold.share.enums.StateEnum;
import com.scaffold.system.domain.entity.system.SystemConfig;
import com.scaffold.system.domain.repository.system.SystemConfigRepository;
import com.scaffold.system.domain.vo.system.BrandPublicVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 从 sys_system_config（group=BRAND）加载可公开品牌设置。
 * <p>
 * 登录前无会话时按 tenantId 过滤；使用 ignore-tenant 分组查询再过滤租户。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BrandSettingsService {

    public static final String GROUP_CODE = "BRAND";

    public static final String CODE_TITLE = "brand.title";
    public static final String CODE_POWERED_BY = "brand.powered-by";
    public static final String CODE_LOGO_SVG = "brand.logo-svg";
    public static final String CODE_FAVICON_SVG = "brand.favicon-svg";
    public static final String CODE_LOGIN_TITLE_SVG = "brand.login-title-svg";
    public static final String CODE_LOGIN_BG_SVG = "brand.login-bg-svg";
    public static final String CODE_SHOW_LOGIN_TITLE_IMAGE = "brand.show-login-title-image";

    private final SystemConfigRepository systemConfigRepository;

    public static boolean isBrandConfig(String groupCode, String configCode) {
        if (groupCode != null && GROUP_CODE.equalsIgnoreCase(groupCode.trim())) {
            return true;
        }
        return configCode != null && configCode.trim().startsWith("brand.");
    }

    /**
     * @param tenantId 与登录一致的租户主键字符串（如 TenantSelectVO.id）
     */
    public BrandPublicVO loadPublic(String tenantId) {
        BrandPublicVO.BrandPublicVOBuilder builder = BrandPublicVO.builder();
        if (tenantId == null || tenantId.isBlank()) {
            return builder.build();
        }
        List<SystemConfig> rows;
        try {
            rows = systemConfigRepository.findAllByGroupCodeIgnoreTenant(GROUP_CODE);
        } catch (Exception ex) {
            log.warn("Load BRAND system configs failed: {}", ex.getMessage());
            return builder.build();
        }
        if (rows == null || rows.isEmpty()) {
            return builder.build();
        }

        String title = null;
        String poweredBy = null;
        String logoSvg = null;
        String faviconSvg = null;
        String loginTitleSvg = null;
        String loginBgSvg = null;
        Boolean showLoginTitleImage = null;

        for (SystemConfig row : rows) {
            if (row == null || row.getConfigCode() == null) {
                continue;
            }
            if (row.getTenantId() == null || !tenantId.equals(row.getTenantId())) {
                continue;
            }
            if (row.getState() != null && row.getState() != StateEnum.ENABLED) {
                continue;
            }
            String code = row.getConfigCode().trim();
            String value = row.getConfigValue();
            switch (code) {
                case CODE_TITLE -> title = trimToNull(value);
                case CODE_POWERED_BY -> poweredBy = trimToNull(value);
                case CODE_LOGO_SVG -> logoSvg = emptyToNull(value);
                case CODE_FAVICON_SVG -> faviconSvg = emptyToNull(value);
                case CODE_LOGIN_TITLE_SVG -> loginTitleSvg = emptyToNull(value);
                case CODE_LOGIN_BG_SVG -> loginBgSvg = emptyToNull(value);
                case CODE_SHOW_LOGIN_TITLE_IMAGE -> showLoginTitleImage = parseBoolean(value);
                default -> {
                    // 非白名单 code 忽略，防止误暴露
                }
            }
        }

        return builder
                .title(title)
                .poweredBy(poweredBy)
                .logoSvg(logoSvg)
                .faviconSvg(faviconSvg)
                .loginTitleSvg(loginTitleSvg)
                .loginBgSvg(loginBgSvg)
                .showLoginTitleImage(showLoginTitleImage)
                .build();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private static Boolean parseBoolean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        if ("true".equalsIgnoreCase(v) || "1".equals(v)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(v) || "0".equals(v)) {
            return Boolean.FALSE;
        }
        return null;
    }
}
