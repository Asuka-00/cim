package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotReworkRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotHistoryRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.ReworkRouteProvider;
import com.scaffold.share.integration.mes.base.response.ReworkEntry;

import lombok.RequiredArgsConstructor;

/**
 * LotReworkApplicationService 进入返工
 * <p>
 * 只接受等待中、还在主流程上的 Lot。次数按根 Lot 的未撤销进入记录统计，拆出去的子 Lot 共用这个上限。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LotReworkApplicationService {

    private final MesTransactionService transactions;
    private final LotRepository lotRepository;
    private final LotHistoryRepository historyRepository;
    private final ReworkRouteProvider reworkRoute;
    private final LotStepPassApplicationService stepPass;
    private final LotHistoryApplicationService history;
    private final Clock clock;

    /**
     * 进入返工分支的第一步。
     *
     * @param lotId Lot
     * @param request 返工分支
     * @throws BusinessException 状态不对、步骤不在范围内，或次数已满
     */
    public void enter(Long lotId, LotReworkRequest request) {
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("ENTER_REWORK");
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

    private void apply(Long lotId, LotReworkRequest request) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (!"WAITING".equals(lot.getStatus()) || lot.getReworkId() != null) {
            throw new BusinessException(MesExecErrorCodes.LOT_REWORK_NOT_READY, "只有主流程上等待中的 Lot 可以进入返工");
        }
        ReworkEntry entry = reworkRoute.find(request.getReworkId())
                .filter(row -> row.enabled() && row.routeVersionId().equals(lot.getRouteVersionId()) && row.firstStepId() != null)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_REWORK_NOT_READY, "返工分支不可用"));
        int seq = lot.getRouteSeqNo() == null ? 0 : lot.getRouteSeqNo();
        if (seq < entry.entryFromSeq() || seq > entry.entryToSeq()) {
            throw new BusinessException(MesExecErrorCodes.LOT_REWORK_OUT_OF_RANGE, "当前步骤不能进入该返工");
        }
        Long root = lot.getRootLotId() == null ? lot.getId() : lot.getRootLotId();
        if (historyRepository.countEnterRework(root, entry.id()) >= entry.maxCount()) {
            throw new BusinessException(MesExecErrorCodes.LOT_REWORK_MAX, "返工次数已达上限");
        }
        lot.setReworkId(entry.id());
        lot.setReworkSeqNo(entry.firstSeqNo());
        lot.setStepId(entry.firstStepId());
        lot.setPassNo(stepPass.arrivalPassNo(lotId, entry.firstStepId()));
        lot.setStepEnterAt(LocalDateTime.now(clock));
        lotRepository.update(lot);
        history.record(lot, "ENTER_REWORK", request.getReasonCode(), null, null, "WAITING", lot.getQty());
    }
}
