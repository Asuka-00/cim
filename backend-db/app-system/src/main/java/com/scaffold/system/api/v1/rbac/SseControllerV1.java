package com.scaffold.system.api.v1.rbac;

import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.system.domain.service.rbac.SseService;
import com.scaffold.system.domain.security.rbac.TokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SseControllerV1控制器类
 * <p>
 * 负责提供SSE实时通知接口
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-10
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/sse")
@RequiredArgsConstructor
@Tag(name = "SseController", description = "SSE实时通知接口")
public class SseControllerV1 {

    private final SseService sseService;
    private final TokenService tokenService;

    @Operation(summary = "建立SSE连接", description = "建立SSE连接用于接收权限变更和重复登录通知")
    @GetMapping("/events")
    public SseEmitter createSseConnection(
            @RequestParam("token") @NonNull String accessToken) {

        // 直接从token中提取用户信息，不依赖Spring Security认证
        String userName;
        try {
            userName = tokenService.getUserName(accessToken);
        } catch (Exception e) {
            log.error("SSE连接 token无效: {}", e.getMessage());
            throw new BusinessException(ErrorCodeConstants.SESSION_NOT_FOUND, "token无效或已过期");
        }

        if (userName == null || userName.isBlank()) {
            log.error("SSE连接 无法从token提取登录名");
            throw new BusinessException(ErrorCodeConstants.SESSION_NOT_FOUND, "token无效");
        }

        log.info("为用户建立SSE连接: {}", userName);

        return sseService.createEmitter(Objects.requireNonNull(userName), accessToken);
    }
}
