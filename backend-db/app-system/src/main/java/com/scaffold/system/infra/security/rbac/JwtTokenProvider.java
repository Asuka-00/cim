package com.scaffold.system.infra.security.rbac;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.system.domain.security.rbac.TokenService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

/**
 * JwtTokenProvider类
 * <p>
 * 负责处理JwtTokenProvider相关核心业务能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider implements TokenService {

    public static final String CLAIM_USER_NAME = "userName";
    public static final String CLAIM_TENANT_ID = "tenantId";
    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String TOKEN_TYPE_ACCESS = TokenService.TOKEN_TYPE_ACCESS;
    public static final String TOKEN_TYPE_REFRESH = TokenService.TOKEN_TYPE_REFRESH;

    private final JwtProperties jwtProperties;

    @Override
    public long getAccessExpireSeconds() {
        return jwtProperties.getAccessExpireSeconds();
    }

    /**
     * 生成访问令牌
     */
    public String generateAccessToken(String userName, String tenantId) {
        return generateToken(userName, tenantId, TOKEN_TYPE_ACCESS, jwtProperties.getAccessExpireSeconds());
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(String userName, String tenantId) {
        return generateToken(userName, tenantId, TOKEN_TYPE_REFRESH, jwtProperties.getRefreshExpireSeconds());
    }

    /**
     * 解析令牌声明
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 获取登录名
     */
    public String getUserName(String token) {
        return parseClaims(token).get(CLAIM_USER_NAME, String.class);
    }

    /**
     * 获取租户标识
     */
    public String getTenantId(String token) {
        return parseClaims(token).get(CLAIM_TENANT_ID, String.class);
    }

    /**
     * 获取令牌类型
     */
    public String getTokenType(String token) {
        return parseClaims(token).get(CLAIM_TOKEN_TYPE, String.class);
    }

    /**
     * 获取过期时间
     */
    public Date getExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    /**
     * 获取剩余秒数
     */
    public long getRemainingSeconds(String token) {
        long seconds = (getExpiration(token).toInstant().toEpochMilli() - Instant.now().toEpochMilli()) / 1000;
        return Math.max(seconds, 0);
    }

    /**
     * 计算令牌标识
     */
    public String tokenId(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessException(ErrorCodeConstants.SYSTEM_ERROR, "无法计算token摘要", ex);
        }
    }

    /**
     * 生成令牌
     */
    private String generateToken(String userName, String tenantId, String tokenType, long expireSeconds) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expireSeconds * 1000);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expireAt)
                .claim(CLAIM_USER_NAME, userName)
                .claim(CLAIM_TENANT_ID, tenantId)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 获取密钥
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
