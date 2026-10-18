package com.scaffold.share.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.scaffold.share.id.AppIdGenerator;
import com.scaffold.share.id.AppIdentifierGenerator;

class AppIdGeneratorAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AppIdGeneratorAutoConfiguration.class);

    @Test
    void shouldRegisterMybatisPlusIdentifierGenerator() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(IdentifierGenerator.class);
            assertThat(context).hasSingleBean(AppIdentifierGenerator.class);

            Number id = context.getBean(IdentifierGenerator.class).nextId(new Object());

            assertThat(id).isInstanceOf(Long.class);
            assertThat(id.longValue()).isBetween(1L, AppIdGenerator.MAX_SAFE_JS_INTEGER);
        });
    }

    @Test
    void shouldConfigureSharedGeneratorWhenIdentifierGeneratorIsOverridden() {
        contextRunner
                .withBean(IdentifierGenerator.class, () -> entity -> 7L)
                .withPropertyValues(
                        "app.id-generator.worker-id=5",
                        "app.id-generator.worker-id-bit-length=6",
                        "app.id-generator.seq-bit-length=6")
                .run(context -> {
                    assertThat(context).hasSingleBean(IdentifierGenerator.class);
                    assertThat(context).doesNotHaveBean(AppIdentifierGenerator.class);

                    long id = AppIdGenerator.nextId();

                    assertThat(id).isBetween(1L, AppIdGenerator.MAX_SAFE_JS_INTEGER);
                    assertThat((id >> 6) & 63).isEqualTo(5);
                });
    }
}
