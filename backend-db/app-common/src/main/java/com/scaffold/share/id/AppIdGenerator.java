package com.scaffold.share.id;

import com.github.yitter.contract.IdGeneratorException;
import com.github.yitter.contract.IdGeneratorOptions;
import com.github.yitter.idgen.YitIdHelper;

/**
 * AppIdGenerator APP 短雪花 ID 生成器
 * <p>
 * 封装 Yitter IdGenerator 初始化和 ID 生成逻辑，统一保证生成值位于 JS Number
 * 与 Redis numeric 精确匹配的安全整数范围内。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-12
 */
public final class AppIdGenerator {

    public static final long MAX_SAFE_JS_INTEGER = AppIdGeneratorProperties.MAX_SAFE_JS_INTEGER;

    private static final Object CONFIG_LOCK = new Object();

    static {
        configure(new AppIdGeneratorProperties());
    }

    private AppIdGenerator() {
    }

    /**
     * 根据配置初始化 Yitter ID 生成器
     */
    public static void configure(AppIdGeneratorProperties properties) {
        AppIdGeneratorProperties safeProperties = properties == null
                ? new AppIdGeneratorProperties()
                : properties;
        safeProperties.validate();

        IdGeneratorOptions options = new IdGeneratorOptions((short) safeProperties.getWorkerId());
        options.BaseTime = safeProperties.getBaseTime();
        options.WorkerId = (short) safeProperties.getWorkerId();
        options.WorkerIdBitLength = (byte) safeProperties.getWorkerIdBitLength();
        options.SeqBitLength = (byte) safeProperties.getSeqBitLength();

        synchronized (CONFIG_LOCK) {
            try {
                YitIdHelper.setIdGenerator(options);
            } catch (IdGeneratorException ex) {
                throw new IllegalStateException("Failed to configure APP ID generator", ex);
            }
        }
    }

    /**
     * 生成下一个 APP 业务 ID
     */
    public static long nextId() {
        long id;
        try {
            id = YitIdHelper.nextId();
        } catch (IdGeneratorException ex) {
            throw new IllegalStateException("Failed to generate APP ID", ex);
        }
        if (id <= 0 || id > MAX_SAFE_JS_INTEGER) {
            throw new IllegalStateException("Generated APP ID is outside JS safe integer range: " + id);
        }
        return id;
    }
}
