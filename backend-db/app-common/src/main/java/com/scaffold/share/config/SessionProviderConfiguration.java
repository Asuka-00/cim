package com.scaffold.share.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.scaffold.share.context.CurrentSessionProvider;

/**
 * SessionProviderConfiguration配置类
 * <p>
 * 负责提供SessionProviderConfiguration相关配置与组件装配
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Configuration
public class SessionProviderConfiguration {

    /**
     * 处理当前会话提供器
     */
    @Bean
    @ConditionalOnMissingBean(CurrentSessionProvider.class)
    public CurrentSessionProvider currentSessionProvider() {
        return new CurrentSessionProvider() {
            @Override
            public String getCurrentUserName() {
                return null;
            }

            @Override
            public String getCurrentTenantId() {
                return null;
            }
        };
    }
}
