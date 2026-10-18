package com.scaffold.mes.base.infra.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MesBaseClockConfig 主数据时钟
 * <p>
 * 给设备状态时长计算提供统一时钟，测试可以换成固定时钟。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Configuration
public class MesBaseClockConfig {

    /**
     * 系统默认时区的时钟。
     *
     * @return 时钟
     */
    @Bean
    public Clock mesBaseClock() {
        return Clock.systemDefaultZone();
    }
}
