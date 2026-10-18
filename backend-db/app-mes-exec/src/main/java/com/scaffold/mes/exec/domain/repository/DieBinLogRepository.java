package com.scaffold.mes.exec.domain.repository;

import com.scaffold.mes.exec.domain.entity.DieBinLog;

/**
 * DieBinLogRepository Die Bin 流水仓储
 * <p>
 * 只追加 hist_mes_die_bin_log。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface DieBinLogRepository {

    /**
     * 插入一条流水。
     *
     * @param log 流水
     */
    void save(DieBinLog log);
}
