package com.scaffold.system.infra.config.system;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.scaffold.system.domain.config.ReleaseProperties;

/**
 * 在线更版配置装配。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-08-12
 */
@Configuration
@EnableConfigurationProperties(ReleaseProperties.class)
public class ReleaseConfiguration {
}
