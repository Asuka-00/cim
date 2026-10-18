package com.scaffold.mes.exec.domain.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotTimeLinkTimer;
import com.scaffold.mes.exec.domain.repository.LotTimeLinkTimerRepository;
import com.scaffold.share.id.AppIdGenerator;
import com.scaffold.share.integration.mes.base.provider.TimeLinkProvider;
import com.scaffold.share.integration.mes.base.response.TimeLinkStart;

import lombok.RequiredArgsConstructor;

/**
 * LotTimeLinkTimerApplicationService 时间链计时
 * <p>
 * 起点步骤出站时，为每条以该序号为起点的约束开一条 ACTIVE 计时。截止时间是出站时间加上最长分钟数。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LotTimeLinkTimerApplicationService {

    private final TimeLinkProvider links;
    private final LotTimeLinkTimerRepository repository;
    private final LotHoldApplicationService holds;

    /**
     * 按当前步骤开计时。返工中的出站不开主流程计时。
     *
     * @param lot 正在离开起点步骤的 Lot
     * @param startAt 出站时间
     */
    public void open(Lot lot, LocalDateTime startAt) {
        if (lot.getReworkId() != null || lot.getRouteSeqNo() == null) {
            return;
        }
        for (TimeLinkStart link : links.startingAt(lot.getRouteVersionId(), lot.getRouteSeqNo())) {
            if (link.maxMinutes() == null || link.maxMinutes() < 1 || link.toSeqNo() == null) {
                continue;
            }
            LotTimeLinkTimer timer = new LotTimeLinkTimer();
            timer.setLotId(lot.getId());
            timer.setTimeLinkId(link.id());
            timer.setRouteVersionId(lot.getRouteVersionId());
            timer.setFromSeqNo(link.fromSeqNo());
            timer.setToSeqNo(link.toSeqNo());
            timer.setStartAt(startAt);
            timer.setDeadlineAt(startAt.plusMinutes(link.maxMinutes()));
            timer.setStatus("ACTIVE");
            timer.setAlertLevel("NONE");
            timer.setAction(link.action());
            timer.setReasonCode(link.reasonCode());
            timer.setStartTxnId(AppIdGenerator.nextId());
            repository.save(timer);
        }
    }

    /**
     * 终点进站时关掉到达该序号的计时。
     *
     * @param lot 正在进站的 Lot
     */
    public void close(Lot lot) {
        if (lot.getRouteSeqNo() == null) {
            return;
        }
        for (LotTimeLinkTimer timer : repository.listActive(lot.getId())) {
            if (lot.getRouteSeqNo().equals(timer.getToSeqNo())) {
                timer.setStatus("DONE");
                repository.update(timer);
            }
        }
    }

    /**
     * 处理已过截止时间的计时。动作为 HOLD 时按原因码扣押，并记成 BREACHED。WARN 只记 WARNED。
     *
     * @param now 当前时间
     * @return 处理了几条
     */
    public int scan(LocalDateTime now) {
        int count = 0;
        for (LotTimeLinkTimer timer : repository.listOverdue(now)) {
            if ("WARN".equals(timer.getAction())) {
                timer.setAlertLevel("WARNED");
            } else {
                String reason = timer.getReasonCode() == null || timer.getReasonCode().isBlank() ? "TIME_LINK"
                        : timer.getReasonCode();
                holds.holdSystem(timer.getLotId(), "TIME_LINK", reason, timer.getId());
                timer.setAlertLevel("BREACHED");
            }
            repository.update(timer);
            count++;
        }
        return count;
    }
}
