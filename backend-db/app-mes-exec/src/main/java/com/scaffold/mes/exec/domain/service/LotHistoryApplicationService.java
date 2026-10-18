package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotHistory;
import com.scaffold.mes.exec.domain.repository.LotHistoryRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * LotHistoryApplicationService Lot 历史
 * <p>
 * 每次事务追加一行。只有 Track-In、Track-Out、Scrap 保留变更前快照，其余类型即使传入也会丢掉。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LotHistoryApplicationService {

    private static final Set<String> REVERSIBLE = Set.of("TRACK_IN", "TRACK_OUT", "SCRAP");

    private final LotHistoryRepository repository;
    private final CurrentSessionProvider session;
    private final Clock clock;

    /**
     * 追加一条历史。数量和状态的“变更前”由调用方传入，因为 Lot 行往往已经改完。
     *
     * @param lot 变更后的 Lot
     * @param txnType 事务类型
     * @param reasonCode 原因码，可空
     * @param remark 备注，可空
     * @param beforeJson 变更前快照，不可撤销类型会被清空
     * @param statusBefore 变更前状态
     * @param qtyBefore 变更前数量
     * @throws BusinessException 事务类型为空
     */
    public void record(Lot lot, String txnType, String reasonCode, String remark, String beforeJson,
            String statusBefore, Long qtyBefore) {
        if (txnType == null || txnType.isBlank()) {
            throw new BusinessException(MesExecErrorCodes.LOT_HISTORY_TYPE_REQUIRED, "历史事务类型不能为空");
        }
        LotHistory history = new LotHistory();
        history.setLotId(lot.getId());
        history.setLotNo(lot.getLotNo());
        history.setTxnType(txnType);
        history.setRouteVersionId(lot.getRouteVersionId());
        history.setRouteSeqNo(lot.getRouteSeqNo());
        history.setStepId(lot.getStepId());
        history.setPassNo(lot.getPassNo());
        history.setReworkId(lot.getReworkId());
        history.setBatchId(lot.getBatchId());
        history.setQtyBefore(qtyBefore);
        history.setQtyAfter(lot.getQty());
        history.setUnitBeforeId(lot.getQtyUnitId());
        history.setUnitAfterId(lot.getQtyUnitId());
        history.setStatusBefore(statusBefore);
        history.setStatusAfter(lot.getStatus());
        history.setReasonCode(reasonCode);
        history.setOperator(session.getCurrentUserNameOrDefault());
        history.setOccurredAt(LocalDateTime.now(clock));
        history.setRemark(remark);
        history.setBeforeJson(REVERSIBLE.contains(txnType) ? beforeJson : null);
        repository.save(history);
    }
}
