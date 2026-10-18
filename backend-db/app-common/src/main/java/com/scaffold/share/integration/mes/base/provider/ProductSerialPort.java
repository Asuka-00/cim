package com.scaffold.share.integration.mes.base.provider;

/**
 * ProductSerialPort 产品是否管控 Unit 序列号
 * <p>
 * 执行模块不能直接读产品。只有打开跟踪的产品才能落序列号。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface ProductSerialPort {

    /**
     * 产品是否打开序列号跟踪。
     *
     * @param productId 产品，可空
     * @return 打开时为 true
     */
    boolean enabled(Long productId);
}
