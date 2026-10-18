package com.scaffold.system.domain.dto.rbac;

import java.io.Serializable;
import java.time.Instant;

import lombok.Data;

/**
 * SSE通知数据传输对象
 * <p>
 * 用于SSE实时通知权限变更和强制登出等事件
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-10
 */
@Data
public class SseNotificationDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 通知类型枚举
     */
    public enum NotificationType {
        /** 权限变更通知 */
        PERMISSION_CHANGED,
        /** 强制登出通知（重复登录） */
        FORCE_LOGOUT,
        /** 连接保活心跳 */
        KEEP_ALIVE
    }

    /** 通知类型 */
    private NotificationType type;

    /** 时间戳（毫秒） */
    private long timestamp;

    /** 通知数据 */
    private Object data;

    /**
     * 创建权限变更通知
     */
    public static SseNotificationDTO permissionChanged(String userName, String message) {
        SseNotificationDTO dto = new SseNotificationDTO();
        dto.setType(NotificationType.PERMISSION_CHANGED);
        dto.setTimestamp(Instant.now().toEpochMilli());
        dto.setData(new PermissionChangeData(userName, message));
        return dto;
    }

    /**
     * 创建强制登出通知
     */
    public static SseNotificationDTO forceLogout(String userName, String reason) {
        SseNotificationDTO dto = new SseNotificationDTO();
        dto.setType(NotificationType.FORCE_LOGOUT);
        dto.setTimestamp(Instant.now().toEpochMilli());
        dto.setData(new ForceLogoutData(userName, reason));
        return dto;
    }

    /**
     * 创建保活心跳通知
     */
    public static SseNotificationDTO keepAlive() {
        SseNotificationDTO dto = new SseNotificationDTO();
        dto.setType(NotificationType.KEEP_ALIVE);
        dto.setTimestamp(Instant.now().toEpochMilli());
        dto.setData(new KeepAliveData("pong"));
        return dto;
    }

    /**
     * 权限变更数据
     */
    @Data
    public static class PermissionChangeData implements Serializable {
        private static final long serialVersionUID = 1L;

        private String userName;
        private String message;

        public PermissionChangeData(String userName, String message) {
            this.userName = userName;
            this.message = message;
        }
    }

    /**
     * 强制登出数据
     */
    @Data
    public static class ForceLogoutData implements Serializable {
        private static final long serialVersionUID = 1L;

        private String userName;
        private String reason;

        public ForceLogoutData(String userName, String reason) {
            this.userName = userName;
            this.reason = reason;
        }
    }

    /**
     * 保活心跳数据
     */
    @Data
    public static class KeepAliveData implements Serializable {
        private static final long serialVersionUID = 1L;

        private String status;

        public KeepAliveData(String status) {
            this.status = status;
        }
    }
}
