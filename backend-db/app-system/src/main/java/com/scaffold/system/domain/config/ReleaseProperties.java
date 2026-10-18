package com.scaffold.system.domain.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 在线更版配置：发布根目录、上传限制、重启延迟与保留版本数。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-08-12
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.release")
public class ReleaseProperties {

    /**
     * 发布根目录（含 current / previous / releases / incoming）。
     */
    private String home = "/opt/scaffold";

    /**
     * 上传 zip 最大字节数。
     */
    private long maxUploadBytes = 536_870_912L;

    /**
     * 激活后延迟退出毫秒数，便于 HTTP 先返回。
     */
    private long restartDelayMs = 2500L;

    /**
     * 保留历史版本数量上限（current / previous 受保护）。
     */
    private int keepCount = 5;
}
