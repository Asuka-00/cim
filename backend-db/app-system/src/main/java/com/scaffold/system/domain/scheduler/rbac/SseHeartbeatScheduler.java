package com.scaffold.system.domain.scheduler.rbac;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.scaffold.share.logging.MdcUtils;
import com.scaffold.system.domain.service.rbac.SseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SSE心跳调度器
 * <p>
 * 定期向所有活跃的SSE连接发送心跳事件，防止连接因空闲超时而断开
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseHeartbeatScheduler {

    /** 心跳发送间隔：5分钟（应小于中间件常见超时配置） */
    private static final long HEARTBEAT_INTERVAL = 5 * 60 * 1000L;

    private final SseService sseService;

    /**
     * 定时发送心跳
     * <p>
     * fixedRate：以上一次任务开始时间为基准计算间隔
     * 适用于心跳这类高频、稳定执行的任务
     */
    @Scheduled(fixedRate = HEARTBEAT_INTERVAL)
    public void sendHeartbeat() {
        MdcUtils.runWithTraceId(null, () -> {
            log.debug("SSE心跳调度任务执行");
            sseService.sendHeartbeatToAll();
        });
    }
}
