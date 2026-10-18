package com.scaffold.app.config;

import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.scaffold.share.db.DatabaseKind;

/**
 * VendorFlywayConfiguration Flyway 按库种类选择脚本目录
 * <p>
 * 覆盖默认 {@code classpath:db/migration}，改为 {@code classpath:db/migration/{mysql|postgresql|oracle|sqlserver}}。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2025-01-29
 */
@Configuration
public class VendorFlywayConfiguration {

    /**
     * 按当前 {@link DatabaseKind} 限定 Flyway 脚本目录。
     */
    @Bean
    public FlywayConfigurationCustomizer vendorFlywayLocations(DatabaseKind databaseKind) {
        String location = "classpath:db/migration/" + databaseKind.folder();
        return configuration -> configuration.locations(location);
    }
}
