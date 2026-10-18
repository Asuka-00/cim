package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.entity.WaferHistory;
import com.scaffold.mes.exec.domain.repository.WaferHistoryRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * WaferHistoryApplicationService Wafer 历史
 * <p>
 * 只在片自身变化时追加一行。Lot 级 Track-In/Track-Out 会被拒绝，避免按片数放大。可撤销类型才保留快照。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class WaferHistoryApplicationService {

    private static final Set<String> TRACK = Set.of("TRACK_IN", "TRACK_OUT");
    private static final Set<String> REVERSIBLE = Set.of("TRACK_IN", "TRACK_OUT", "SCRAP", "SLOT_CHANGE");

    private final WaferHistoryRepository repository;
    private final CurrentSessionProvider session;
    private final Clock clock;

    /**
     * 追加一条 Wafer 历史。
     *
     * @param wafer 变更后的片
     * @param lot 发生时所属 Lot
     * @param txnType 事务类型
     * @param waferLevelTrack 为 true 时才允许写 Track-In/Track-Out
     * @param slotBefore 变更前 Slot
     * @param statusBefore 变更前状态
     * @param reasonCode 原因码，可空
     * @param remark 备注，可空
     * @param beforeJson 变更前快照，不可撤销类型会被清空
     * @throws BusinessException 类型为空，或 Lot 级 Track 试图逐片写入
     */
    public void record(Wafer wafer, Lot lot, String txnType, boolean waferLevelTrack, Integer slotBefore,
            String statusBefore, String reasonCode, String remark, String beforeJson) {
        if (txnType == null || txnType.isBlank()) {
            throw new BusinessException(MesExecErrorCodes.WAFER_HISTORY_TYPE_REQUIRED, "Wafer 历史事务类型不能为空");
        }
        if (TRACK.contains(txnType) && !waferLevelTrack) {
            throw new BusinessException(MesExecErrorCodes.WAFER_HISTORY_LOT_TRACK, "Lot 级 Track 不逐片写 Wafer 历史");
        }
        WaferHistory history = new WaferHistory();
        history.setWaferId(wafer.getId());
        history.setWaferCode(wafer.getWaferCode());
        history.setLotId(lot.getId());
        history.setLotNo(lot.getLotNo());
        history.setTxnType(txnType);
        history.setRouteSeqNo(lot.getRouteSeqNo());
        history.setStepId(lot.getStepId());
        history.setPassNo(lot.getPassNo());
        history.setSlotNoBefore(slotBefore);
        history.setSlotNoAfter(wafer.getSlotNo());
        history.setStatusBefore(statusBefore);
        history.setStatusAfter(wafer.getStatus());
        history.setReasonCode(reasonCode);
        history.setOperator(session.getCurrentUserNameOrDefault());
        history.setOccurredAt(LocalDateTime.now(clock));
        history.setRemark(remark);
        history.setBeforeJson(REVERSIBLE.contains(txnType) ? beforeJson : null);
        repository.save(history);
    }
}
