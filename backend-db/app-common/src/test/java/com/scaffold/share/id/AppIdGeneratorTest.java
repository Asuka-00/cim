package com.scaffold.share.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;

class AppIdGeneratorTest {

    @Test
    void nextIdShouldBePositiveMonotonicAndJsSafe() {
        AppIdGenerator.configure(defaultProperties());

        long first = AppIdGenerator.nextId();
        long second = AppIdGenerator.nextId();

        assertThat(first).isPositive();
        assertThat(second).isGreaterThan(first);
        assertThat(first).isLessThanOrEqualTo(AppIdGenerator.MAX_SAFE_JS_INTEGER);
        assertThat(second).isLessThanOrEqualTo(AppIdGenerator.MAX_SAFE_JS_INTEGER);
    }

    @Test
    void propertiesShouldRejectWorkerIdOutsideBitLength() {
        AppIdGeneratorProperties properties = defaultProperties();
        properties.setWorkerIdBitLength(2);
        properties.setWorkerId(4);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("worker-id must be between 0 and 3");
    }

    @Test
    void identifierGeneratorShouldReturnJsSafeLong() {
        IdentifierGenerator generator = new AppIdentifierGenerator(defaultProperties());

        Number id = generator.nextId(new Object());

        assertThat(id).isInstanceOf(Long.class);
        assertThat(id.longValue()).isPositive();
        assertThat(id.longValue()).isLessThanOrEqualTo(AppIdGenerator.MAX_SAFE_JS_INTEGER);
    }

    private static AppIdGeneratorProperties defaultProperties() {
        AppIdGeneratorProperties properties = new AppIdGeneratorProperties();
        properties.setWorkerId(0);
        properties.setBaseTime(1_767_196_800_000L);
        properties.setWorkerIdBitLength(6);
        properties.setSeqBitLength(6);
        return properties;
    }
}
