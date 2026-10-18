package com.scaffold.share.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SwaggerRedirectConfig配置类
 * <p>
 * 负责提供SwaggerRedirectConfig相关配置与组件装配
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Configuration
public class SwaggerRedirectConfig implements WebMvcConfigurer {

    /**
     * 添加视图控制器
     */
    @Override
    public void addViewControllers(@NonNull ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/swagger-ui.html");
        registry.addRedirectViewController("/swagger-ui", "/swagger-ui.html");
    }
}
