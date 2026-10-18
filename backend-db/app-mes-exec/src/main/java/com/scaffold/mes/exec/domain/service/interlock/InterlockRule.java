package com.scaffold.mes.exec.domain.service.interlock;

/**
 * InterlockRule 一条进站检查
 * <p>
 * BLOCK 会回滚进站。SKIP 表示这条规则不适用于当前站点。M3 的六条规则不可停用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface InterlockRule {

    /**
     * 规则编码。
     *
     * @return 编码
     */
    String code();

    /**
     * 检查一次进站。
     *
     * @param context 当前 Lot 和资源判定
     * @return BLOCK、WARN 或 SKIP
     */
    InterlockHit evaluate(TrackInContext context);
}
