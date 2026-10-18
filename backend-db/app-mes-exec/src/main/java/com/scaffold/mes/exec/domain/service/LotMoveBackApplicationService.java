package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotMoveBackRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.RouteNextProvider;
import com.scaffold.share.integration.mes.base.response.RouteStepNext;

import lombok.RequiredArgsConstructor;

/**
 * LotMoveBackApplicationService 退回上一步
 * <p>
 * 等待中的主流程 Lot 回到前一个序号重走。不回减 pass_count，新的 pass_no 是已完成次数加 1。到达后执行预约动作。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LotMoveBackApplicationService {

    private final MesTransactionService transactions;
    private final LotRepository lotRepository;
    private final RouteNextProvider routeNext;
    private final LotStepPassApplicationService stepPass;
    private final LotHistoryApplicationService history;
    private final FutureActionApplicationService futureActions;
    private final Clock clock;

    /**
     * 退回上一步。
     *
     * @param lotId Lot
     * @param request 原因码
     * @throws BusinessException 不是等待状态、人在返工里、已经是第一步，或原因为空
     */
    public void moveBack(Long lotId, LotMoveBackRequest request) {
        if (request.getReasonCode() == null || request.getReasonCode().isBlank()) {
            throw new BusinessException(MesExecErrorCodes.LOT_MOVE_BACK_REASON, "退回必须填写原因");
        }
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("MOVE_BACK");
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

    private void apply(Long lotId, LotMoveBackRequest request) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (!"WAITING".equals(lot.getStatus()) || lot.getReworkId() != null) {
            throw new BusinessException(MesExecErrorCodes.LOT_MOVE_BACK_NOT_READY, "只有主流程上等待中的 Lot 可以退回");
        }
        RouteStepNext previous = routeNext.previous(lot.getRouteVersionId(), lot.getRouteSeqNo())
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_MOVE_BACK_NO_PREVIOUS, "已经是第一步"));
        lot.setRouteSeqNo(previous.seqNo());
        lot.setStepId(previous.stepId());
        lot.setPassNo(stepPass.arrivalPassNo(lotId, previous.stepId()));
        lot.setStepEnterAt(LocalDateTime.now(clock));
        lot.setLocationType("AREA");
        lot.setLocationId(previous.areaId());
        lotRepository.update(lot);
        history.record(lot, "MOVE_BACK", request.getReasonCode(), request.getRemark(), null, "WAITING", lot.getQty());
        futureActions.onArrive(lot);
    }
}
