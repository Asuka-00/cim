package com.scaffold.share.config;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.scaffold.share.jackson.TrimStringModule;
import com.scaffold.share.web.filter.ParameterTrimmingFilter;
import com.scaffold.share.web.resolver.PathVariableTrimmingArgumentResolver;

/**
 * WebMvcAutoConfiguration配置类
 * <p>
 * 负责提供WebMvcAutoConfiguration相关配置与组件装配
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@AutoConfiguration
@EnableConfigurationProperties(WebMvcProperties.class)
public class WebMvcAutoConfiguration implements WebMvcConfigurer {

    /**
     * 参数去空格过滤器Bean
     * <p>
     * 过滤查询参数和表单数据的前后空格
     */
    @Bean
    public ParameterTrimmingFilter parameterTrimmingFilter() {
        return new ParameterTrimmingFilter();
    }

    /**
     * Jackson去空格模块Bean
     * <p>
     * 处理JSON请求体中字符串字段的去空格
     */
    @Bean
    public TrimStringModule trimStringModule() {
        return new TrimStringModule();
    }

    /**
     * 注册路径参数去空格解析器
     */
    @Override
    public void addArgumentResolvers(
            @NonNull List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new PathVariableTrimmingArgumentResolver());
    }
}
