package com.scaffold.mes.exec.domain.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.WaferBindRequest;
import com.scaffold.mes.exec.domain.dto.WaferBindRequest.WaferPiece;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * WaferApplicationService Wafer 应用服务
 * <p>
 * 给已开批的 Lot 绑定片。片数必须等于 Lot 数量，存活片数回写 wafer_count。不锁工单。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class WaferApplicationService {

    private static final Set<String> PURPOSES = Set.of("PRODUCT", "MONITOR", "DUMMY", "CONTROL");

    private final WaferRepository repository;
    private final LotRepository lotRepository;
    private final WaferHistoryApplicationService history;
    private final GenealogyLinkApplicationService genealogy;

    /**
     * 绑定一批 Wafer。
     *
     * @param lotId Lot
     * @param request 指定编号或自动生成的片数
     * @throws BusinessException Lot 不存在、片数不一致、编号重复或用途不合法
     */
    @Transactional
    public void bind(Long lotId, WaferBindRequest request) {
        lotRepository.lock(lotId);
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (lot.getWaferCount() != null && lot.getWaferCount() > 0) {
            throw new BusinessException(MesExecErrorCodes.WAFER_ALREADY_BOUND, "该 Lot 已经绑定过 Wafer");
        }
        String purpose = request.getPurpose() == null || request.getPurpose().isBlank() ? "PRODUCT" : request.getPurpose();
        if (!PURPOSES.contains(purpose)) {
            throw new BusinessException(MesExecErrorCodes.WAFER_PURPOSE_INVALID, "Wafer 用途不合法: " + purpose);
        }
        List<WaferPiece> pieces = pieces(lot, request);
        if (pieces.size() != lot.getQty()) {
            throw new BusinessException(MesExecErrorCodes.WAFER_QTY_MISMATCH, "Wafer 片数必须等于 Lot 数量");
        }
        Set<String> seen = new HashSet<>();
        int slot = 1;
        for (WaferPiece piece : pieces) {
            if (!seen.add(piece.getWaferCode())) {
                throw new BusinessException(MesExecErrorCodes.WAFER_CODE_DUPLICATE, "Wafer 编号重复: " + piece.getWaferCode());
            }
            if (repository.findByCode(piece.getWaferCode()).isPresent()) {
                throw new BusinessException(MesExecErrorCodes.WAFER_CODE_EXISTS, "Wafer 编号已存在: " + piece.getWaferCode());
            }
            Wafer wafer = new Wafer();
            wafer.setWaferCode(piece.getWaferCode());
            wafer.setLotId(lotId);
            wafer.setSlotNo(slot++);
            wafer.setStatus("NORMAL");
            wafer.setPurpose(purpose);
            wafer.setVendorWaferId(piece.getVendorWaferId());
            wafer.setDummyUseCount(0);
            wafer.setVersion(0);
            repository.save(wafer);
            history.record(wafer, lot, "CREATE", false, null, null, null, null, null);
            genealogy.link("LOT", lotId, lot.getLotNo(), "WAFER", wafer.getId(), wafer.getWaferCode(),
                    "CREATE", lot.getStepId(), BigDecimal.ONE, null);
        }
        lot.setWaferCount(pieces.size());
        lotRepository.update(lot);
    }

    private static List<WaferPiece> pieces(Lot lot, WaferBindRequest request) {
        if (request.getWafers() != null && !request.getWafers().isEmpty()) {
            return request.getWafers();
        }
        int count = request.getCount() == null ? 0 : request.getCount();
        return java.util.stream.IntStream.rangeClosed(1, count).mapToObj(slot -> {
            WaferPiece piece = new WaferPiece();
            piece.setWaferCode(lot.getLotNo() + "." + slot);
            return piece;
        }).toList();
    }
}
