package com.scaffold.mes.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * MesProperties MES 运行参数
 * <p>
 * 绑定 app.mes。关闭后不检查数据库种类,也不跑事件轮询。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.mes")
public class MesProperties {

    /** 关闭后不跑调度,启动时也不检查数据库种类。 */
    private boolean enabled = true;

    /** 领域事件轮询间隔,单位毫秒。 */
    private long eventPollMs = 2000;
}
