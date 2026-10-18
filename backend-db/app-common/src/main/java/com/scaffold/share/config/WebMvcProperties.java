package com.scaffold.share.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * WebMvcProperties配置类
 * <p>
 * 负责提供WebMvcProperties相关配置与组件装配
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
@ConfigurationProperties(prefix = "web.mvc")
public class WebMvcProperties {

    /** 是否启用自动参数去空格 */
    private boolean trimmingEnabled = true;

    /** 需要排除去空格的路径（如Swagger相关端点） */
    private List<String> excludePaths = List.of(
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/favicon.ico");
}
