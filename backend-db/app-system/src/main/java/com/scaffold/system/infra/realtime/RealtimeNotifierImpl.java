package com.scaffold.system.infra.realtime;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.scaffold.share.integration.realtime.RealtimeEvent;
import com.scaffold.share.integration.realtime.RealtimeNotifier;
import com.scaffold.system.domain.dto.rbac.SseNotificationDTO;
import com.scaffold.system.domain.service.rbac.SseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RealtimeNotifierImpl 实时通知 SPI 的 SSE 实现。
 * <p>
 * 将 common 层 {@link RealtimeNotifier} 桥接到 {@link SseService}。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-07-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeNotifierImpl implements RealtimeNotifier {

    private final SseService sseService;

    @Override
    public void publishToUser(String userName, RealtimeEvent event) {
        if (!StringUtils.hasText(userName) || event == null) {
            return;
        }
        SseNotificationDTO notification = toSseNotification(event);
        if (notification == null) {
            return;
        }
        sseService.sendToUser(userName, notification);
    }

    @Override
    public void publishToAllOnline(RealtimeEvent event) {
        if (event == null) {
            return;
        }
        SseNotificationDTO notification = toSseNotification(event);
        if (notification == null) {
            return;
        }
        sseService.broadcastToAllOnline(notification);
    }

    private SseNotificationDTO toSseNotification(RealtimeEvent event) {
        if (!StringUtils.hasText(event.getType())) {
            log.warn("RealtimeEvent type 为空，跳过推送");
            return null;
        }
        SseNotificationDTO.NotificationType type;
        try {
            type = SseNotificationDTO.NotificationType.valueOf(event.getType());
        } catch (IllegalArgumentException ex) {
            log.warn("未知 RealtimeEvent type: {}，跳过推送", event.getType());
            return null;
        }
        SseNotificationDTO dto = new SseNotificationDTO();
        dto.setType(type);
        dto.setTimestamp(event.getTimestamp() > 0 ? event.getTimestamp() : System.currentTimeMillis());
        dto.setData(event.getData());
        return dto;
    }
}
