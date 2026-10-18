package com.scaffold.system.domain.service.system;

import java.util.Locale;
import java.util.regex.Pattern;

import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.exception.BusinessException;

/**
 * 系统配置中 SVG 文本的轻量校验（防脚本、限体积）。
 */
public final class SvgConfigValidator {

    public static final String VALUE_TYPE_SVG = "svg";
    public static final int MAX_LENGTH = 65535;

    private static final Pattern SCRIPT = Pattern.compile("(?i)<\\s*script");
    private static final Pattern JS_URL = Pattern.compile("(?i)javascript\\s*:");
    private static final Pattern ON_EVENT = Pattern.compile("(?i)\\son[a-z]+\\s*=");

    private SvgConfigValidator() {
    }

    public static boolean isSvgType(String valueType) {
        return valueType != null && VALUE_TYPE_SVG.equalsIgnoreCase(valueType.trim());
    }

    /**
     * 空值视为合法（回退静态默认）；非空则必须为可安全渲染的 SVG 片段。
     */
    public static void validateOrThrow(String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String value = raw.trim();
        if (value.length() > MAX_LENGTH) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM,
                    "SVG 配置值长度不能超过 " + MAX_LENGTH);
        }
        String lower = value.toLowerCase(Locale.ROOT);
        // 允许可选 XML 声明 / doctype 后的 <svg
        int svgIdx = lower.indexOf("<svg");
        if (svgIdx < 0) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "SVG 配置必须以 <svg 开头");
        }
        if (SCRIPT.matcher(value).find()) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "SVG 不允许包含 script");
        }
        if (JS_URL.matcher(value).find()) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "SVG 不允许 javascript: URL");
        }
        if (ON_EVENT.matcher(value).find()) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "SVG 不允许内联事件处理器");
        }
    }
}
