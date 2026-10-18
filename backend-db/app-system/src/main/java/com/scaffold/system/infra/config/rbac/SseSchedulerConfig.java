package com.scaffold.system.infra.config.rbac;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SSE调度器配置类
 * <p>
 * 启用Spring定时任务调度，用于SSE心跳机制
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-10
 */
@Configuration
@EnableScheduling
public class SseSchedulerConfig {
}
