package com.scaffold.mes.exec.domain.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.scaffold.mes.exec.domain.entity.LotTimeLinkTimer;

/**
 * LotTimeLinkTimerRepository 时间链计时仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface LotTimeLinkTimerRepository {

    /**
     * 插入一条计时。
     *
     * @param timer 计时
     */
    void save(LotTimeLinkTimer timer);

    /**
     * 列出某个 Lot 仍在计时的记录。
     *
     * @param lotId Lot
     * @return ACTIVE 计时
     */
    List<LotTimeLinkTimer> listActive(Long lotId);

    /**
     * 列出已过截止时间、且还没记成 BREACHED 的计时。
     *
     * @param now 当前时间
     * @return 待处理计时
     */
    List<LotTimeLinkTimer> listOverdue(LocalDateTime now);

    /**
     * 更新计时。终点进站时改为 DONE。
     *
     * @param timer 已加载的计时
     */
    void update(LotTimeLinkTimer timer);
}
