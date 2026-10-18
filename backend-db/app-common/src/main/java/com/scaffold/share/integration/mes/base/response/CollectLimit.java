package com.scaffold.share.integration.mes.base.response;

import java.math.BigDecimal;

/**
 * CollectLimit 采集规格上下限
 *
 * @param lsl 下限。空表示不限
 * @param usl 上限。空表示不限
 * @param autoHold 超规格时是否自动 Hold
 * @param reasonCode 自动 Hold 原因码
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record CollectLimit(BigDecimal lsl, BigDecimal usl, boolean autoHold, String reasonCode) {

    /**
     * 只有上下限、不自动 Hold。
     *
     * @param lsl 下限
     * @param usl 上限
     */
    public CollectLimit(BigDecimal lsl, BigDecimal usl) {
        this(lsl, usl, false, null);
    }
}
