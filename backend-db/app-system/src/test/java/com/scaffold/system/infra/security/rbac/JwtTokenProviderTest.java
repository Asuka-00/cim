package com.scaffold.system.infra.security.rbac;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * JwtTokenProviderTest测试类
 * <p>
 * 负责验证JwtTokenProviderTest相关功能行为与稳定性
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */

class JwtTokenProviderTest {

    @Test
    void shouldContainTenantIdClaim() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("ChangeMeToA32ByteOrLongerSecretKeyForHS256");
        jwtProperties.setAccessExpireSeconds(1800L);
        jwtProperties.setRefreshExpireSeconds(604800L);

        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtProperties", jwtProperties);

        String accessToken = jwtTokenProvider.generateAccessToken("u001", "t001");

        assertEquals("u001", jwtTokenProvider.getUserName(accessToken));
        assertEquals("t001", jwtTokenProvider.getTenantId(accessToken));
        assertEquals(JwtTokenProvider.TOKEN_TYPE_ACCESS, jwtTokenProvider.getTokenType(accessToken));
    }
}
