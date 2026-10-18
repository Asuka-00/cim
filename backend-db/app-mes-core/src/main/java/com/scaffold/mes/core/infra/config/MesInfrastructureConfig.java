package com.scaffold.mes.core.infra.config;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.scaffold.mes.core.config.MesProperties;
import com.scaffold.share.db.DatabaseKind;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

/**
 * MesInfrastructureConfig MES 基础设施配置
 * <p>
 * 装配调度锁与启动检查。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
@EnableConfigurationProperties(MesProperties.class)
public class MesInfrastructureConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        // ponytail: 自写 JDBC 锁。官方 shedlock-provider-jdbc-template 的 usingDbTime() 在依赖能下载时换上。
        return new JdbcShedLockProvider(new org.springframework.jdbc.core.JdbcTemplate(dataSource));
    }

    @Bean
    public MesStartupCheck mesStartupCheck(MesProperties properties, DatabaseKind databaseKind) {
        return new MesStartupCheck(properties, databaseKind);
    }
}
