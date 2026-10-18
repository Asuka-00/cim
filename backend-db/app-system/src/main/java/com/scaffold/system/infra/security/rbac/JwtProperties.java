package com.scaffold.system.infra.security.rbac;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * JwtProperties配置类
 * <p>
 * 负责提供JwtProperties相关配置与组件装配
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {

    private String secret;

    private long accessExpireSeconds;

    private long refreshExpireSeconds;
}
