package com.scaffold.mes.warehouse.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.warehouse.domain.MesWarehouseErrorCodes;
import com.scaffold.mes.warehouse.domain.dto.InspectionRequest;
import com.scaffold.mes.warehouse.domain.entity.IncomingInspection;
import com.scaffold.mes.warehouse.domain.entity.MaterialLot;
import com.scaffold.mes.warehouse.domain.repository.ReceiptStore;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * IncomingInspectionApplicationService 来料检验
 * <p>
 * 只接受待检批。合格和特采变为 AVAILABLE，不合格变为 QUARANTINE。特采必须写说明。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class IncomingInspectionApplicationService {

    private static final Set<String> RESULTS = Set.of("PASS", "FAIL", "CONCESSION");

    private final ReceiptStore store;
    private final CurrentSessionProvider session;
    private final Clock clock;

    /**
     * 记录检验并改物料批状态。
     *
     * @param materialLotId 物料批
     * @param request 结论和说明
     * @throws BusinessException 不是待检、结论无效，或特采没有说明
     */
    @Transactional
    public void inspect(Long materialLotId, InspectionRequest request) {
        if (request.getResult() == null || !RESULTS.contains(request.getResult())) {
            throw new BusinessException(MesWarehouseErrorCodes.INSPECTION_RESULT, "检验结论无效");
        }
        if ("CONCESSION".equals(request.getResult()) && (request.getRemark() == null || request.getRemark().isBlank())) {
            throw new BusinessException(MesWarehouseErrorCodes.INSPECTION_REMARK, "特采必须填写说明");
        }
        MaterialLot lot = store.findLotById(materialLotId)
                .orElseThrow(() -> new BusinessException(MesWarehouseErrorCodes.INSPECTION_STATUS, "物料批不存在"));
        if (!"PENDING_QC".equals(lot.getStatus())) {
            throw new BusinessException(MesWarehouseErrorCodes.INSPECTION_STATUS, "只有待检物料批可以检验");
        }
        lot.setQcResult(request.getResult());
        lot.setStatus("FAIL".equals(request.getResult()) ? "QUARANTINE" : "AVAILABLE");
        store.updateLot(lot);
        IncomingInspection inspection = new IncomingInspection();
        inspection.setMaterialLotId(materialLotId);
        inspection.setResult(request.getResult());
        inspection.setInspector(session.getCurrentUserNameOrDefault());
        inspection.setInspectedAt(LocalDateTime.now(clock));
        inspection.setRemark(request.getRemark());
        store.saveInspection(inspection);
    }
}
