package com.scaffold.share.integration.realtime;

/**
 * RealtimeNotifier 实时事件发布 SPI。
 * <p>
 * 定义在 common，供各模块在运行期发布实时事件。
 * 实现位于 app-system，桥接 SSE 等推送通道。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-07-09
 */
public interface RealtimeNotifier {

    /**
     * 向指定用户的所有在线连接推送事件。
     *
     * @param userName 用户登录名
     * @param event    实时事件
     */
    void publishToUser(String userName, RealtimeEvent event);

    /**
     * 向当前进程内所有在线连接广播事件。
     *
     * @param event 实时事件
     */
    void publishToAllOnline(RealtimeEvent event);
}
