package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.FutureActionRequest;
import com.scaffold.mes.exec.domain.dto.LotHoldRequest;
import com.scaffold.mes.exec.domain.dto.LotPriorityRequest;
import com.scaffold.mes.exec.domain.entity.FutureAction;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.FutureActionRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * FutureActionApplicationService 预约动作
 * <p>
 * 到达主流程步骤时执行 ON_ARRIVE。返工步骤不触发。Hold 和改优先级在到站事务里完成，失败的动作记为 FAILED，不回滚到站。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class FutureActionApplicationService {

    private static final Set<String> TYPES = Set.of("HOLD", "CHANGE_PRIORITY");

    private final FutureActionRepository repository;
    private final LotRepository lotRepository;
    private final LotHoldApplicationService holds;
    private final LotPriorityApplicationService priorities;
    private final Clock clock;

    /**
     * 列出某个 Lot 的全部预约。
     *
     * @param lotId Lot
     * @return 按步骤序号排列
     */
    public List<FutureAction> list(Long lotId) {
        return repository.listByLot(lotId);
    }

    /**
     * 预约一个到站动作。
     *
     * @param lotId Lot
     * @param request 步骤、类型和参数
     * @throws BusinessException Lot 不存在或类型不支持
     */
    public void schedule(Long lotId, FutureActionRequest request) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (!TYPES.contains(request.getActionType())) {
            throw new BusinessException(MesExecErrorCodes.FUTURE_ACTION_TYPE_INVALID, "预约动作类型不支持");
        }
        FutureAction action = new FutureAction();
        action.setLotId(lotId);
        action.setRouteVersionId(lot.getRouteVersionId());
        action.setRouteSeqNo(request.getRouteSeqNo());
        action.setPassNo(request.getPassNo());
        action.setTiming("ON_ARRIVE");
        action.setActionType(request.getActionType());
        action.setActionParams(request.getActionParams());
        action.setStatus("PENDING");
        repository.save(action);
    }

    /**
     * 执行当前主流程步骤上到期的预约。
     *
     * @param lot 刚到达的 Lot
     */
    public void onArrive(Lot lot) {
        if (lot.getReworkId() != null || lot.getRouteSeqNo() == null) {
            return;
        }
        for (FutureAction action : repository.listDue(lot.getId(), lot.getRouteVersionId(), lot.getRouteSeqNo())) {
            if (action.getPassNo() != null && !action.getPassNo().equals(lot.getPassNo())) {
                continue;
            }
            try {
                run(lot, action);
                action.setStatus("DONE");
                action.setResultMessage(null);
            } catch (RuntimeException ex) {
                action.setStatus("FAILED");
                action.setResultMessage(ex.getMessage());
            }
            action.setExecutedAt(LocalDateTime.now(clock));
            repository.update(action);
        }
    }

    private void run(Lot lot, FutureAction action) {
        if ("HOLD".equals(action.getActionType())) {
            LotHoldRequest request = new LotHoldRequest();
            request.setReasonCode(action.getActionParams());
            holds.hold(lot.getId(), request);
            return;
        }
        LotPriorityRequest request = new LotPriorityRequest();
        request.setPriority(Integer.valueOf(action.getActionParams()));
        priorities.change(lot.getId(), request);
    }
}
