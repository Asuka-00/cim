package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotRouteChangeRequest;
import com.scaffold.mes.exec.domain.entity.FutureAction;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotRouteChange;
import com.scaffold.mes.exec.domain.repository.FutureActionRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.LotRouteChangeRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.RouteNextProvider;
import com.scaffold.share.integration.mes.base.response.RouteStepNext;

import lombok.RequiredArgsConstructor;

/**
 * LotRouteChangeApplicationService 改 Route
 * <p>
 * 只改等待中的主流程 Lot。旧版本上未执行的预约改为 CANCELLED。到达新步骤后执行新版本上的预约。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LotRouteChangeApplicationService {

    private final MesTransactionService transactions;
    private final LotRepository lotRepository;
    private final RouteNextProvider routeNext;
    private final FutureActionRepository futureActionRepository;
    private final LotRouteChangeRepository changeRepository;
    private final LotStepPassApplicationService stepPass;
    private final LotHistoryApplicationService history;
    private final FutureActionApplicationService futureActions;
    private final CurrentSessionProvider session;
    private final Clock clock;

    /**
     * 改到新的 Route 版本和步骤。
     *
     * @param lotId Lot
     * @param request 新版本、步骤和原因
     * @return 被取消的旧预约条数
     * @throws BusinessException 不是等待状态、目标步骤不存在，或原因为空
     */
    public int change(Long lotId, LotRouteChangeRequest request) {
        if (request.getReasonCode() == null || request.getReasonCode().isBlank()) {
            throw new BusinessException(MesExecErrorCodes.LOT_ROUTE_CHANGE_REASON, "改 Route 必须填写原因");
        }
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("CHANGE_ROUTE");
        command.setObjectType("LOT");
        command.setObjectId(lotId);
        command.setReasonCode(request.getReasonCode());
        command.setSource("TERMINAL");
        command.setLocks(List.of(new MesLockTarget("LOT", lotId)));
        return transactions.execute(command, () -> apply(lotId, request), Integer.class);
    }

    private int apply(Long lotId, LotRouteChangeRequest request) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (!"WAITING".equals(lot.getStatus()) || lot.getReworkId() != null) {
            throw new BusinessException(MesExecErrorCodes.LOT_ROUTE_CHANGE_NOT_READY, "只有主流程上等待中的 Lot 可以改 Route");
        }
        RouteStepNext target = routeNext.at(request.getRouteVersionId(), request.getRouteSeqNo())
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_ROUTE_CHANGE_STEP_MISSING, "新 Route 上没有目标步骤"));
        List<FutureAction> pending = futureActionRepository.listPending(lotId, lot.getRouteVersionId());
        for (FutureAction action : pending) {
            action.setStatus("CANCELLED");
            action.setResultMessage("改 Route 后取消");
            futureActionRepository.update(action);
        }
        LotRouteChange change = new LotRouteChange();
        change.setLotId(lotId);
        change.setFromProductVersionId(lot.getProductVersionId());
        change.setToProductVersionId(request.getProductVersionId() == null ? lot.getProductVersionId() : request.getProductVersionId());
        change.setFromRouteVersionId(lot.getRouteVersionId());
        change.setToRouteVersionId(request.getRouteVersionId());
        change.setFromSeqNo(lot.getRouteSeqNo());
        change.setToSeqNo(target.seqNo());
        change.setReasonCode(request.getReasonCode());
        change.setOperator(session.getCurrentUserNameOrDefault());
        change.setOccurredAt(LocalDateTime.now(clock));
        changeRepository.save(change);
        lot.setRouteVersionId(request.getRouteVersionId());
        if (request.getProductVersionId() != null) {
            lot.setProductVersionId(request.getProductVersionId());
        }
        lot.setRouteSeqNo(target.seqNo());
        lot.setStepId(target.stepId());
        lot.setPassNo(stepPass.arrivalPassNo(lotId, target.stepId()));
        lot.setStepEnterAt(LocalDateTime.now(clock));
        lot.setLocationType("AREA");
        lot.setLocationId(target.areaId());
        lotRepository.update(lot);
        history.record(lot, "CHANGE_ROUTE", request.getReasonCode(), null, null, "WAITING", lot.getQty());
        futureActions.onArrive(lot);
        return pending.size();
    }
}
