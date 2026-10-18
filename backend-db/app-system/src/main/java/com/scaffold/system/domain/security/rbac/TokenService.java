package com.scaffold.system.domain.security.rbac;

import java.util.Date;

import io.jsonwebtoken.Claims;

/**
 * TokenService令牌服务接口
 * <p>
 * 定义系统应用服务所需的令牌生成、解析与会话有效期查询能力，具体实现由基础设施层提供。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-07-10
 */
public interface TokenService {

    String TOKEN_TYPE_ACCESS = "access";
    String TOKEN_TYPE_REFRESH = "refresh";

    /**
     * 生成访问令牌
     */
    String generateAccessToken(String userName, String tenantId);

    /**
     * 生成刷新令牌
     */
    String generateRefreshToken(String userName, String tenantId);

    /**
     * 解析令牌声明
     */
    Claims parseClaims(String token);

    /**
     * 获取令牌中的用户账号
     */
    String getUserName(String token);

    /**
     * 获取令牌中的租户标识
     */
    String getTenantId(String token);

    /**
     * 获取令牌类型
     */
    String getTokenType(String token);

    /**
     * 获取令牌过期时间
     */
    Date getExpiration(String token);

    /**
     * 获取令牌剩余有效秒数
     */
    long getRemainingSeconds(String token);

    /**
     * 计算令牌唯一标识
     */
    String tokenId(String token);

    /**
     * 获取访问令牌有效期秒数
     */
    long getAccessExpireSeconds();
}
