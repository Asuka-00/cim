package com.scaffold.share.web.resolver;

import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.PathVariableMethodArgumentResolver;

/**
 * PathVariableTrimmingArgumentResolver解析器类
 * <p>
 * 负责处理PathVariableTrimmingArgumentResolver相关参数解析与转换逻辑
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
public class PathVariableTrimmingArgumentResolver implements HandlerMethodArgumentResolver {

    private final PathVariableMethodArgumentResolver delegate = new PathVariableMethodArgumentResolver();

    /**
     * 判断是否支持参数
     */
    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        return parameter.hasParameterAnnotation(PathVariable.class)
                && String.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            @NonNull NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory) throws Exception {
        Object value = delegate.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
        if (value instanceof String str) {
            String trimmed = str.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
        return value;
    }
}
