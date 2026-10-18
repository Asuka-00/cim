package com.scaffold.mes.exec.domain.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotPriorityRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * LotPriorityApplicationService 修改 Lot 优先级
 * <p>
 * 只改在制 Lot，Hold 不阻止。终态不能改。不锁工单。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LotPriorityApplicationService {

    private static final Set<String> OPEN = Set.of("WAITING", "RUNNING");

    private final MesTransactionService transactions;
    private final LotRepository lotRepository;
    private final LotHistoryApplicationService history;

    /**
     * 修改优先级。
     *
     * @param lotId Lot
     * @param request 新优先级
     * @throws BusinessException 超出 1 到 9，或 Lot 已终态
     */
    public void change(Long lotId, LotPriorityRequest request) {
        if (request.getPriority() == null || request.getPriority() < 1 || request.getPriority() > 9) {
            throw new BusinessException(MesExecErrorCodes.LOT_PRIORITY_INVALID, "优先级必须在 1 到 9");
        }
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("PRIORITY");
        command.setObjectType("LOT");
        command.setObjectId(lotId);
        command.setSource("TERMINAL");
        command.setLocks(List.of(new MesLockTarget("LOT", lotId)));
        transactions.execute(command, () -> {
            apply(lotId, request.getPriority());
            return Boolean.TRUE;
        }, Boolean.class);
    }

    private void apply(Long lotId, int priority) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (!OPEN.contains(lot.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.LOT_PRIORITY_CLOSED, "终态 Lot 不能改优先级");
        }
        String beforeStatus = lot.getStatus();
        lot.setPriority(priority);
        lotRepository.update(lot);
        history.record(lot, "PRIORITY", null, null, null, beforeStatus, lot.getQty());
    }
}
