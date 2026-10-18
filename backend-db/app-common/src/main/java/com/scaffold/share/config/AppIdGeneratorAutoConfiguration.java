package com.scaffold.share.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.scaffold.share.id.AppIdGenerator;
import com.scaffold.share.id.AppIdGeneratorProperties;
import com.scaffold.share.id.AppIdentifierGenerator;

/**
 * AppIdGeneratorAutoConfiguration 短雪花 ID 自动配置类
 * <p>
 * 负责装配 APP 统一短雪花 ID 生成器，并接管 MyBatis-Plus ASSIGN_ID 主键生成策略。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-12
 */
@Configuration
@EnableConfigurationProperties(AppIdGeneratorProperties.class)
public class AppIdGeneratorAutoConfiguration {

    /**
     * 初始化 APP 统一短雪花 ID 生成器
     */
    @Bean
    public InitializingBean appIdGeneratorInitializer(AppIdGeneratorProperties properties) {
        return () -> AppIdGenerator.configure(properties);
    }

    /**
     * 创建 MyBatis-Plus ASSIGN_ID 主键生成器
     */
    @Bean
    @ConditionalOnMissingBean(IdentifierGenerator.class)
    public IdentifierGenerator appIdentifierGenerator(AppIdGeneratorProperties properties) {
        return new AppIdentifierGenerator(properties);
    }
}
