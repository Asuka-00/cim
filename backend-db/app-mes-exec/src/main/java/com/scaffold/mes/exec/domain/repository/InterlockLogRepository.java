package com.scaffold.mes.exec.domain.repository;

import com.scaffold.mes.exec.domain.entity.InterlockLog;

/**
 * InterlockLogRepository 进站阻断日志仓储
 * <p>
 * 只追加 hist_mes_interlock_log。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface InterlockLogRepository {

    /**
     * 插入一条阻断日志。
     *
     * @param log 日志
     */
    void save(InterlockLog log);
}
