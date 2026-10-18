package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotForceMoveRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.ConfirmPort;
import com.scaffold.share.integration.mes.base.provider.RouteNextProvider;
import com.scaffold.share.integration.mes.base.response.RouteStepNext;

import lombok.RequiredArgsConstructor;

/**
 * LotForceMoveApplicationService 强制跳站
 * <p>
 * 等待中的主流程 Lot 可以跳到 Route 上任意一步。被跳过的站点不增加 pass_count，也不做物料反冲。到达后执行预约动作。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LotForceMoveApplicationService {

    private final MesTransactionService transactions;
    private final LotRepository lotRepository;
    private final RouteNextProvider routeNext;
    private final LotStepPassApplicationService stepPass;
    private final LotHistoryApplicationService history;
    private final FutureActionApplicationService futureActions;
    private final ConfirmPort confirms;
    private final Clock clock;

    /**
     * 跳到指定步骤。
     *
     * @param lotId Lot
     * @param request 目标序号和原因
     * @throws BusinessException 不是等待状态、人在返工里、目标步骤不存在，或原因为空
     */
    public void move(Long lotId, LotForceMoveRequest request) {
        if (request.getReasonCode() == null || request.getReasonCode().isBlank()) {
            throw new BusinessException(MesExecErrorCodes.LOT_FORCE_MOVE_REASON, "强制跳站必须填写原因");
        }
        if (!confirms.allowed("FORCE_MOVE", "LOT", lotId)) {
            throw new BusinessException(MesExecErrorCodes.CONFIRM_REQUIRED, "强制跳站还没有完成确认");
        }
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("FORCE_MOVE");
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

    private void apply(Long lotId, LotForceMoveRequest request) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (!"WAITING".equals(lot.getStatus()) || lot.getReworkId() != null) {
            throw new BusinessException(MesExecErrorCodes.LOT_FORCE_MOVE_NOT_READY, "只有主流程上等待中的 Lot 可以强制跳站");
        }
        if (request.getRouteSeqNo().equals(lot.getRouteSeqNo())) {
            throw new BusinessException(MesExecErrorCodes.LOT_FORCE_MOVE_STEP_MISSING, "已经在目标步骤");
        }
        RouteStepNext target = routeNext.at(lot.getRouteVersionId(), request.getRouteSeqNo())
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_FORCE_MOVE_STEP_MISSING, "目标步骤不在当前 Route 上"));
        int from = lot.getRouteSeqNo() == null ? 0 : lot.getRouteSeqNo();
        lot.setRouteSeqNo(target.seqNo());
        lot.setStepId(target.stepId());
        lot.setPassNo(stepPass.arrivalPassNo(lotId, target.stepId()));
        lot.setStepEnterAt(LocalDateTime.now(clock));
        lot.setLocationType("AREA");
        lot.setLocationId(target.areaId());
        lotRepository.update(lot);
        String remark = "from=" + from + ",to=" + target.seqNo()
                + (request.getRemark() == null ? "" : "," + request.getRemark());
        history.record(lot, "FORCE_MOVE", request.getReasonCode(), remark, null, "WAITING", lot.getQty());
        futureActions.onArrive(lot);
    }
}
