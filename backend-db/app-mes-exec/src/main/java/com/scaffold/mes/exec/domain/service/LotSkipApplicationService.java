package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotSkipRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.RouteNextProvider;
import com.scaffold.share.integration.mes.base.response.RouteStepNext;

import lombok.RequiredArgsConstructor;

/**
 * LotSkipApplicationService 跳站
 * <p>
 * 等待中的 Lot 跳到下一个序号。被跳过的站点不增加 pass_count。到达新站点时重新记下进入时间。不锁工单。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LotSkipApplicationService {

    private final MesTransactionService transactions;
    private final LotRepository lotRepository;
    private final RouteNextProvider routeNext;
    private final LotStepPassApplicationService stepPass;
    private final LotHistoryApplicationService history;
    private final FutureActionApplicationService futureActions;
    private final Clock clock;

    /**
     * 跳到下一步。
     *
     * @param lotId Lot
     * @param request 原因码
     * @throws BusinessException 不是等待状态、没有下一步，或原因为空
     */
    public void skip(Long lotId, LotSkipRequest request) {
        if (request.getReasonCode() == null || request.getReasonCode().isBlank()) {
            throw new BusinessException(MesExecErrorCodes.LOT_SKIP_REASON_REQUIRED, "跳站必须填写原因");
        }
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("SKIP");
        command.setObjectType("LOT");
        command.setObjectId(lotId);
        command.setReasonCode(request.getReasonCode());
        command.setSource("TERMINAL");
        command.setLocks(List.of(new MesLockTarget("LOT", lotId)));
        transactions.execute(command, () -> {
            apply(lotId, request);
            return Boolean.TRUE;
        }, Boolean.class);
    }

    private void apply(Long lotId, LotSkipRequest request) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (!"WAITING".equals(lot.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.LOT_SKIP_NOT_WAITING, "只有等待中的 Lot 可以跳站");
        }
        RouteStepNext next = routeNext.next(lot.getRouteVersionId(), lot.getRouteSeqNo())
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_SKIP_NO_NEXT, "已经没有下一步"));
        lot.setRouteSeqNo(next.seqNo());
        lot.setStepId(next.stepId());
        lot.setPassNo(stepPass.arrivalPassNo(lotId, next.stepId()));
        lot.setStepEnterAt(LocalDateTime.now(clock));
        lot.setLocationType("AREA");
        lot.setLocationId(next.areaId());
        lotRepository.update(lot);
        history.record(lot, "SKIP", request.getReasonCode(), request.getRemark(), null, "WAITING", lot.getQty());
        futureActions.onArrive(lot);
    }
}
