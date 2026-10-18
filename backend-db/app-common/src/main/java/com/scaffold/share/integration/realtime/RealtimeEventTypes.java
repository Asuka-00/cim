package com.scaffold.share.integration.realtime;

/**
 * RealtimeEventTypes 实时事件类型常量。
 * <p>
 * 与前端 {@code SseEventType}、SSE 命名事件名保持一致。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-07-09
 */
public final class RealtimeEventTypes {

    private RealtimeEventTypes() {
    }

    /** 权限变更通知 */
    public static final String PERMISSION_CHANGED = "PERMISSION_CHANGED";

    /** 强制登出通知 */
    public static final String FORCE_LOGOUT = "FORCE_LOGOUT";

    /** 连接保活心跳 */
    public static final String KEEP_ALIVE = "KEEP_ALIVE";
}
