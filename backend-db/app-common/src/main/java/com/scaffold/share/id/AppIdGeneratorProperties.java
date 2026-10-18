package com.scaffold.share.id;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

/**
 * AppIdGeneratorProperties 短雪花 ID 生成器配置类
 * <p>
 * 负责绑定 app.id-generator 配置项，并校验 Yitter worker、序列号和起始时间等参数。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-12
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.id-generator")
public class AppIdGeneratorProperties {

    /**
     * JS Number 可安全表示的最大整数。
     */
    public static final long MAX_SAFE_JS_INTEGER = 9_007_199_254_740_991L;

    /**
     * Yitter 起始时间戳，默认 2026-01-01 00:00:00 UTC。
     */
    private long baseTime = 1_767_196_800_000L;

    /**
     * 当前实例 worker ID。
     */
    private int workerId = 0;

    /**
     * worker ID 位数，默认支持 64 个实例。
     */
    private int workerIdBitLength = 6;

    /**
     * 序列号位数，默认单实例约 64k IDs/s。
     */
    private int seqBitLength = 6;

    /**
     * 校验短雪花 ID 生成器配置
     */
    @PostConstruct
    public void validate() {
        if (baseTime <= 0) {
            throw new IllegalArgumentException("app.id-generator.base-time must be positive");
        }
        if (workerIdBitLength <= 0 || workerIdBitLength > 15) {
            throw new IllegalArgumentException("app.id-generator.worker-id-bit-length must be between 1 and 15");
        }
        if (seqBitLength <= 0 || seqBitLength > 15) {
            throw new IllegalArgumentException("app.id-generator.seq-bit-length must be between 1 and 15");
        }
        long maxWorkerId = (1L << workerIdBitLength) - 1;
        if (workerId < 0 || workerId > maxWorkerId) {
            throw new IllegalArgumentException(
                    "app.id-generator.worker-id must be between 0 and " + maxWorkerId);
        }
    }
}
