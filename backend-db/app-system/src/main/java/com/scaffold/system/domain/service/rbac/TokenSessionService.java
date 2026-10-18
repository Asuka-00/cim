package com.scaffold.system.domain.service.rbac;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.scaffold.share.constants.RedisKeyConstants;
import com.scaffold.share.utils.RedisKeyBuilder;
import com.scaffold.system.domain.security.rbac.TokenService;

import lombok.RequiredArgsConstructor;

/**
 * TokenSessionService服务类
 * <p>
 * 负责处理TokenSessionService相关业务编排与领域逻辑
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Service
@RequiredArgsConstructor
public class TokenSessionService {

    private final StringRedisTemplate stringRedisTemplate;

    private final TokenService tokenService;

    /**
     * 构建认证键
     */
    private String buildAuthKey(String keyType, String userName) {
        String safeKeyType = Objects.requireNonNull(keyType, "keyType must not be null");
        String safeUserName = Objects.requireNonNull(userName, "userName must not be null");
        return Objects.requireNonNull(RedisKeyBuilder.buildAuthKey(safeKeyType, safeUserName));
    }

    /**
     * 构建黑名单键
     */
    private String buildBlacklistKey(String tokenId) {
        String safeTokenId = Objects.requireNonNull(tokenId, "tokenId must not be null");
        return Objects.requireNonNull(RedisKeyBuilder.buildBlacklistKey(safeTokenId));
    }

    /**
     * 绑定活动令牌
     */
    public void bindActiveTokens(String userName, String accessToken, String refreshToken) {
        String safeUserName = Objects.requireNonNull(userName, "userName must not be null");
        String safeAccessToken = Objects.requireNonNull(accessToken, "accessToken must not be null");
        String safeRefreshToken = Objects.requireNonNull(refreshToken, "refreshToken must not be null");
        String accessKey = Objects.requireNonNull(buildAuthKey(RedisKeyConstants.AUTH_USER_ACCESS, safeUserName));
        String refreshKey = Objects.requireNonNull(buildAuthKey(RedisKeyConstants.AUTH_USER_REFRESH, safeUserName));
        long accessTtl = Math.max(1, tokenService.getRemainingSeconds(safeAccessToken));
        long refreshTtl = Math.max(1, tokenService.getRemainingSeconds(safeRefreshToken));
        stringRedisTemplate.opsForValue().set(
                accessKey,
                safeAccessToken,
                accessTtl,
                TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(
                refreshKey,
                safeRefreshToken,
                refreshTtl,
                TimeUnit.SECONDS);
    }

    /**
     * 获取活动访问令牌
     */
    @Nullable
    public String getActiveAccessToken(String userName) {
        String safeUserName = Objects.requireNonNull(userName, "userName must not be null");
        String accessKey = Objects.requireNonNull(buildAuthKey(RedisKeyConstants.AUTH_USER_ACCESS, safeUserName));
        return stringRedisTemplate.opsForValue().get(accessKey);
    }

    /**
     * 获取活动刷新令牌
     */
    @Nullable
    public String getActiveRefreshToken(String userName) {
        String safeUserName = Objects.requireNonNull(userName, "userName must not be null");
        String refreshKey = Objects.requireNonNull(buildAuthKey(RedisKeyConstants.AUTH_USER_REFRESH, safeUserName));
        return stringRedisTemplate.opsForValue().get(refreshKey);
    }

    /**
     * 更新活动访问令牌
     */
    public void updateActiveAccessToken(String userName, String accessToken) {
        String safeUserName = Objects.requireNonNull(userName, "userName must not be null");
        String safeAccessToken = Objects.requireNonNull(accessToken, "accessToken must not be null");
        String accessKey = Objects.requireNonNull(buildAuthKey(RedisKeyConstants.AUTH_USER_ACCESS, safeUserName));
        long accessTtl = Math.max(1, tokenService.getRemainingSeconds(safeAccessToken));
        stringRedisTemplate.opsForValue().set(
                accessKey,
                safeAccessToken,
                accessTtl,
                TimeUnit.SECONDS);
    }

    /**
     * 移除活动访问令牌
     */
    public void removeActiveAccessToken(String userName) {
        String safeUserName = Objects.requireNonNull(userName, "userName must not be null");
        String accessKey = Objects.requireNonNull(buildAuthKey(RedisKeyConstants.AUTH_USER_ACCESS, safeUserName));
        stringRedisTemplate.delete(accessKey);
    }

    /**
     * 加入黑名单令牌
     */
    public void blacklistToken(String token) {
        String safeToken = Objects.requireNonNull(token, "token must not be null");
        String tokenId = Objects.requireNonNull(tokenService.tokenId(safeToken), "tokenId must not be null");
        String blacklistKey = Objects.requireNonNull(buildBlacklistKey(tokenId));
        long ttl = tokenService.getRemainingSeconds(safeToken);
        if (ttl <= 0) {
            return;
        }
        stringRedisTemplate.opsForValue().set(
                blacklistKey,
                "1",
                ttl,
                TimeUnit.SECONDS);
    }

    /**
     * 判断令牌是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        String safeToken = Objects.requireNonNull(token, "token must not be null");
        String tokenId = Objects.requireNonNull(tokenService.tokenId(safeToken), "tokenId must not be null");
        String blacklistKey = Objects.requireNonNull(buildBlacklistKey(tokenId));
        Boolean exists = stringRedisTemplate.hasKey(blacklistKey);
        return Boolean.TRUE.equals(exists);
    }
}
