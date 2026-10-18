package com.scaffold.share.db;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import lombok.extern.slf4j.Slf4j;

/**
 * DatabaseKindAutoConfiguration 数据库种类自动配置
 * <p>
 * 从 JDBC URL 或 {@code app.db.type} 识别数据库类型。分页方言在 {@code MybatisPlusConfig}。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2025-01-29
 */
@Slf4j
@AutoConfiguration
public class DatabaseKindAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DatabaseKind databaseKind(
            @Value("${spring.datasource.url:}") String jdbcUrl,
            @Value("${app.db.type:}") String explicitType) {
        DatabaseKind kind = DatabaseKindResolver.resolve(jdbcUrl, explicitType);
        log.info("DatabaseKind initialized: {}", kind);
        return kind;
    }
}
