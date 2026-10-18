package com.scaffold.system.domain.service.rbac;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.constants.RedisKeyConstants;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.utils.RedisKeyBuilder;
import com.scaffold.system.domain.vo.rbac.LoginVO;
import com.scaffold.system.domain.repository.rbac.OAuth2RegistrationRepository;

import lombok.RequiredArgsConstructor;

/**
 * OAuth2 登录一次性换票
 * <p>
 * 将本系统 LoginVO 暂存 Redis，前端凭 ticket 换取本地 JWT，避免 token 出现在浏览器 URL。
 * 不缓存 IdP token。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-07-17
 */
@Service
@RequiredArgsConstructor
public class OAuth2TicketService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final OAuth2RegistrationRepository clientRegistrationRepository;

    /**
     * 创建一次性 ticket 并缓存登录结果
     */
    public String createTicket(LoginVO loginVO) {
        Objects.requireNonNull(loginVO, "loginVO must not be null");
        String ticket = UUID.randomUUID().toString().replace("-", "");
        String key = RedisKeyBuilder.buildAuthKey(RedisKeyConstants.AUTH_OAUTH2_TICKET, ticket);
        try {
            String payload = objectMapper.writeValueAsString(loginVO);
            long ttl = Math.max(1, clientRegistrationRepository.currentSettings().getTicketTtlSeconds());
            stringRedisTemplate.opsForValue().set(key, payload, ttl, TimeUnit.SECONDS);
            return ticket;
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCodeConstants.SYSTEM_ERROR, "OAuth2 ticket 序列化失败", ex);
        }
    }

    /**
     * 消费 ticket（一次性），返回登录结果
     */
    public LoginVO consumeTicket(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            throw new BusinessException(ErrorCodeConstants.OAUTH2_TICKET_INVALID, "OAuth2 ticket 无效");
        }
        String key = RedisKeyBuilder.buildAuthKey(RedisKeyConstants.AUTH_OAUTH2_TICKET, ticket);
        String payload = stringRedisTemplate.opsForValue().getAndDelete(key);
        if (payload == null || payload.isBlank()) {
            throw new BusinessException(ErrorCodeConstants.OAUTH2_TICKET_INVALID, "OAuth2 ticket 无效或已过期");
        }
        try {
            return objectMapper.readValue(payload, LoginVO.class);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCodeConstants.SYSTEM_ERROR, "OAuth2 ticket 反序列化失败", ex);
        }
    }
}
