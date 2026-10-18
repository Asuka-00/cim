package com.scaffold.mes.exec.domain.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.TrackInRequest;
import com.scaffold.mes.exec.domain.dto.WaferTrackRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * WaferTrackApplicationService 片级进出站
 * <p>
 * 没被选中的片留在原步骤，拆成子 Lot。选中的片跟原 Lot 继续进站或出站，并写片级历史。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class WaferTrackApplicationService {

    private static final Set<String> ALIVE = Set.of("NORMAL", "RUNNING");

    private final LotRepository lotRepository;
    private final WaferRepository waferRepository;
    private final LotSplitApplicationService split;
    private final TrackInApplicationService trackIn;
    private final TrackOutApplicationService trackOut;
    private final WaferHistoryApplicationService waferHistory;

    /**
     * 让选中的片进站。
     *
     * @param lotId Lot
     * @param request 选中的片和设备
     * @throws BusinessException 选中的片不属于该 Lot
     */
    public void trackIn(Long lotId, WaferTrackRequest request) {
        List<Wafer> selected = selected(lotId, request.getWaferIds());
        List<Long> leave = leave(lotId, request.getWaferIds());
        if (!leave.isEmpty()) {
            split.splitLeaving(lotId, leave);
        }
        TrackInRequest track = new TrackInRequest();
        track.setStepId(request.getStepId());
        track.setEquipmentId(request.getEquipmentId());
        track.setRecipeVersionId(request.getRecipeVersionId());
        trackIn.trackIn(lotId, track);
        record(lotId, selected, "TRACK_IN");
    }

    /**
     * 让选中的片出站。
     *
     * @param lotId Lot
     * @param request 选中的片
     * @throws BusinessException 选中的片不属于该 Lot
     */
    public void trackOut(Long lotId, WaferTrackRequest request) {
        List<Wafer> selected = selected(lotId, request.getWaferIds());
        List<Long> leave = leave(lotId, request.getWaferIds());
        if (!leave.isEmpty()) {
            split.splitLeaving(lotId, leave);
        }
        trackOut.trackOut(lotId, null);
        record(lotId, selected, "TRACK_OUT");
    }

    private void record(Long lotId, List<Wafer> selected, String txnType) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        for (Wafer wafer : selected) {
            waferHistory.record(wafer, lot, txnType, true, wafer.getSlotNo(), wafer.getStatus(), null, null, null);
        }
    }

    private List<Wafer> selected(Long lotId, List<Long> waferIds) {
        List<Wafer> alive = alive(lotId);
        List<Wafer> selected = alive.stream().filter(wafer -> waferIds.contains(wafer.getId())).toList();
        if (selected.isEmpty() || selected.size() != waferIds.size()) {
            throw new BusinessException(MesExecErrorCodes.WAFER_TRACK_SELECTION, "片级进出站必须选择该 Lot 上的存活片");
        }
        return selected;
    }

    private List<Long> leave(Long lotId, List<Long> waferIds) {
        return alive(lotId).stream().map(Wafer::getId).filter(id -> !waferIds.contains(id)).toList();
    }

    private List<Wafer> alive(Long lotId) {
        return waferRepository.listByLot(lotId).stream().filter(wafer -> ALIVE.contains(wafer.getStatus())).toList();
    }
}
