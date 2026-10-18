package com.scaffold.mes.exec.domain.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.integration.mes.base.provider.QueueTimeProvider;
import com.scaffold.share.integration.mes.base.response.QueueTimeLimit;

import lombok.RequiredArgsConstructor;

/**
 * QueueTimeScanApplicationService 排队时间扫描
 * <p>
 * 只看未 Hold 的 WAITING Lot。超过步骤上限且动作不是 WARN 时，按 QUEUE_TIME 扣押。每批单独走 Hold 事务。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class QueueTimeScanApplicationService {

    private final LotRepository lotRepository;
    private final QueueTimeProvider queueTimes;
    private final LotHoldApplicationService holds;

    /**
     * 扫描并扣押已超时的 Lot。
     *
     * @param now 当前时间
     * @return 新扣押的批数
     */
    public int scan(LocalDateTime now) {
        int count = 0;
        for (Lot lot : lotRepository.listWaiting(null, null)) {
            if (overdue(lot, now)) {
                holds.holdSystem(lot.getId(), "QUEUE_TIME", "QUEUE_TIME", lot.getStepId());
                count++;
            }
        }
        return count;
    }

    private boolean overdue(Lot lot, LocalDateTime now) {
        if (lot.getStepEnterAt() == null) {
            return false;
        }
        QueueTimeLimit limit = queueTimes.limit(lot.getRouteVersionId(), lot.getRouteSeqNo());
        if (limit.maxMinutes() == null || "WARN".equals(limit.action())) {
            return false;
        }
        return lot.getStepEnterAt().plusMinutes(limit.maxMinutes()).isBefore(now);
    }
}
