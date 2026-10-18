package com.scaffold.share.vo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ResultTest {

    @Test
    void failStoresErrorParams() {
        Result<Object> result = Result.fail(500042, "detail message", Map.of("boundBy", "Product Flow"));

        assertThat(result.getCode()).isEqualTo(500042);
        assertThat(result.getMessage()).isEqualTo("detail message");
        assertThat(result.getErrorParams()).containsEntry("boundBy", "Product Flow");
    }
}
