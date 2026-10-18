package com.scaffold.system.domain.service.rbac;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.scaffold.share.utils.JsonWriter;
import com.scaffold.system.domain.dto.rbac.SseNotificationDTO;
import com.scaffold.system.domain.security.rbac.TokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SseService服务类
 * <p>
 * 负责管理SSE连接和推送通知
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

    /** SSE连接超时时间：30分钟（与accessToken过期时间一致） */
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    /** 用户Code到SseEmitter集合的映射，支持同一用户多标签页/多窗口在线 */
    private final Map<String, Set<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    private final TokenService tokenService;

    private final JsonWriter jsonWriter;

    /**
     * 为用户创建SSE连接
     *
     * @param userName    用户编码
     * @param accessToken 访问令牌（用于验证用户身份）
     * @return SseEmitter
     */
    public SseEmitter createEmitter(@NonNull String userName, @NonNull String accessToken) {
        // 验证token是否属于该用户
        String tokenUserName = tokenService.getUserName(accessToken);
        if (!userName.equals(tokenUserName)) {
            throw new IllegalArgumentException("Token does not match user");
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 注册回调
        emitter.onCompletion(() -> {
            log.info("SSE连接完成，用户: {}", userName);
            cleanupUserConnection(userName, emitter);
        });

        emitter.onTimeout(() -> {
            log.info("SSE连接超时，用户: {}", userName);
            cleanupUserConnection(userName, emitter);
        });

        emitter.onError(e -> {
            if (isDisconnectedClient(e)) {
                log.debug("SSE连接断开，用户: {}, 错误: {}", userName, e.getMessage());
            } else {
                log.error("SSE连接异常，用户: {}, 错误: {}", userName, e.getMessage());
            }
            cleanupUserConnection(userName, emitter);
        });

        userEmitters.computeIfAbsent(userName, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);

        // 发送连接成功事件
        try {
            SseNotificationDTO notification = new SseNotificationDTO();
            notification.setType(SseNotificationDTO.NotificationType.PERMISSION_CHANGED);
            notification.setTimestamp(System.currentTimeMillis());
            notification.setData(Map.of("status", "connected", "message", "SSE连接已建立"));

            String data = jsonWriter.write(notification);
            emitter.send(SseEmitter.event()
                    .name("connection")
                    .data(Objects.requireNonNull(data)));
        } catch (IOException | RuntimeException e) {
            log.error("发送初始SSE事件失败", e);
        }

        log.info("为用户创建SSE连接: {}", userName);
        return emitter;
    }

    /**
     * 移除用户的SSE连接
     */
    public void removeEmitter(@NonNull String userName) {
        Set<SseEmitter> emitters = userEmitters.remove(userName);
        if (emitters != null) {
            emitters.forEach(emitter -> {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.debug("关闭emitter时出错: {}", e.getMessage());
                }
            });
        }
        log.info("移除用户SSE连接: {}", userName);
    }

    /**
     * 发送通知给指定用户
     */
    public void sendToUser(String userName, SseNotificationDTO notification) {
        Set<SseEmitter> emitters = userEmitters.get(userName);
        if (emitters == null || emitters.isEmpty()) {
            log.warn("用户没有活跃的SSE连接: {}", userName);
            return;
        }

        String data;
        try {
            data = jsonWriter.write(notification);
        } catch (RuntimeException e) {
            log.error("SSE通知序列化失败，用户: {}, 类型: {}", userName, notification.getType(), e);
            return;
        }
        String eventName = Objects
                .requireNonNullElse(notification.getType(), SseNotificationDTO.NotificationType.PERMISSION_CHANGED)
                .name();

        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(Objects.requireNonNull(eventName))
                        .data(Objects.requireNonNull(data)));
            } catch (IOException e) {
                log.warn("发送SSE通知失败，移除连接: {}", userName);
                cleanupUserConnection(userName, emitter);
                try {
                    emitter.complete();
                } catch (Exception ex) {
                    log.debug("关闭emitter时出错: {}", ex.getMessage());
                }
            }
        });
        log.info("SSE通知已发送给用户: {}, 类型: {}, 连接数: {}", userName, notification.getType(), emitters.size());
    }

    /**
     * 广播权限变更通知给指定用户
     */
    public void broadcastPermissionChange(String userName, String message) {
        SseNotificationDTO notification = SseNotificationDTO.permissionChanged(userName, message);
        sendToUser(userName, notification);
    }

    /**
     * 向当前进程内所有在线连接广播通知。
     *
     * @param notification 通知内容
     */
    public void broadcastToAllOnline(SseNotificationDTO notification) {
        if (notification == null || userEmitters.isEmpty()) {
            return;
        }
        // 复制 key 快照，避免遍历时连接集合变更导致 ConcurrentModification
        userEmitters.keySet().forEach(userName -> sendToUser(userName, notification));
        log.info("SSE 通知已广播给在线用户, 类型: {}, 用户数: {}", notification.getType(), userEmitters.size());
    }

    /**
     * 发送强制登出通知
     */
    public void sendForceLogout(String userName, String reason) {
        SseNotificationDTO notification = SseNotificationDTO.forceLogout(userName, reason);
        sendToUser(userName, notification);
    }

    /**
     * 用户登录时调用，用于检测重复登录
     * 如果用户已有SSE连接，发送强制登出通知并关闭
     */
    public void onUserLogin(@NonNull String userName) {
        Set<SseEmitter> existingEmitters = userEmitters.get(userName);
        if (existingEmitters != null && !existingEmitters.isEmpty()) {
            log.info("检测到用户重复登录，发送强制登出通知: {}", userName);
            // 先发送通知，让客户端有机会处理
            sendForceLogout(userName, "您的账号在另一处登录，您已被强制登出");

            // 短暂延迟后移除连接
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            removeEmitter(userName);
        }
    }

    /**
     * 用户登出时清理SSE连接
     */
    public void onUserLogout(@NonNull String userName) {
        removeEmitter(userName);
    }

    /**
     * 清理用户连接
     */
    private void cleanupUserConnection(String userName, SseEmitter emitter) {
        Set<SseEmitter> emitters = userEmitters.get(userName);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            userEmitters.remove(userName, emitters);
        }
    }

    /**
     * 客户端主动断开 / 容器通知断连，属预期生命周期，不应记 ERROR。
     */
    static boolean isDisconnectedClient(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("disconnected client")
                        || lower.contains("broken pipe")
                        || lower.contains("connection reset")
                        || lower.contains("connection aborted")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * 检查用户是否有活跃的SSE连接
     */
    public boolean hasActiveConnection(@NonNull String userName) {
        Set<SseEmitter> emitters = userEmitters.get(userName);
        return emitters != null && !emitters.isEmpty();
    }

    /**
     * 向所有活跃连接发送心跳保活
     * <p>
     * 由SseHeartbeatScheduler定时调用，防止连接因空闲超时而断开
     */
    public void sendHeartbeatToAll() {
        if (userEmitters.isEmpty()) {
            return;
        }

        SseNotificationDTO heartbeat = SseNotificationDTO.keepAlive();
        String data;
        try {
            data = jsonWriter.write(heartbeat);
        } catch (RuntimeException e) {
            log.error("心跳消息序列化失败", e);
            return;
        }

        userEmitters.forEach((userName, emitters) -> emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("keepalive")
                        .data(Objects.requireNonNull(data)));
            } catch (IOException e) {
                log.warn("心跳发送失败，移除连接: {}", userName);
                cleanupUserConnection(userName, emitter);
                try {
                    emitter.complete();
                } catch (Exception ex) {
                    log.debug("关闭emitter时出错: {}", ex.getMessage());
                }
            }
        }));

        log.debug("已向{}个活跃连接发送心跳", userEmitters.size());
    }
}
