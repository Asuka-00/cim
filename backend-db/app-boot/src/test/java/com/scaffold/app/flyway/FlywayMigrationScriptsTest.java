package com.scaffold.app.flyway;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * 不依赖 Docker：确认 Flyway 脚本已打进测试 classpath。
 */
class FlywayMigrationScriptsTest {

    @Test
    void versionedBaselineAndRepeatableSeedAreOnClasspath() {
        for (String vendor : new String[] {"mysql", "postgresql", "oracle", "sqlserver"}) {
            assertThat(new ClassPathResource("db/migration/" + vendor + "/V1_0_0__baseline.sql").isReadable())
                    .as(vendor + " baseline")
                    .isTrue();
            assertThat(new ClassPathResource("db/migration/" + vendor + "/V1_0_1__user_multi_tenant.sql").isReadable())
                    .as(vendor + " multi-tenant")
                    .isTrue();
            assertThat(new ClassPathResource("db/migration/" + vendor + "/V1_0_2__user_platform_admin.sql").isReadable())
                    .as(vendor + " platform-admin")
                    .isTrue();
            assertThat(new ClassPathResource("db/migration/" + vendor + "/R__system_data.sql").isReadable())
                    .as(vendor + " seed")
                    .isTrue();
        }
    }

    @Test
    void mesScriptsExistOnlyForMysql() {
        assertThat(new ClassPathResource("db/migration/mysql/V1_1_0__mes_m0.sql").isReadable()).isTrue();
        assertThat(new ClassPathResource("db/migration/mysql/R__system_data_mes.sql").isReadable()).isTrue();
        for (String vendor : new String[] {"postgresql", "oracle", "sqlserver"}) {
            assertThat(new ClassPathResource("db/migration/" + vendor + "/V1_1_0__mes_m0.sql").exists())
                    .as(vendor + " must not ship MES ddl")
                    .isFalse();
        }
    }

    @Test
    void mesM0ScriptCreatesEveryDesignTable() throws Exception {
        String sql = new ClassPathResource("db/migration/mysql/V1_1_0__mes_m0.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        List<String> tables = List.of(
                "mes_sequence_rule",
                "mes_sequence_counter",
                "mes_object_lock",
                "hist_mes_transaction",
                "mes_domain_event",
                "mes_domain_event_delivery",
                "hist_mes_txn_reversal",
                "mes_alert",
                "shedlock");
        for (String table : tables) {
            assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `" + table + "`");
        }
    }
}
