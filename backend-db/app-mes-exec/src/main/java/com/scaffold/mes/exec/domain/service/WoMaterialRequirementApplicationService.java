package com.scaffold.mes.exec.domain.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.scaffold.mes.exec.domain.entity.WoMaterialRequirement;
import com.scaffold.mes.exec.domain.entity.WorkOrder;
import com.scaffold.mes.exec.domain.repository.WoMaterialRequirementRepository;
import com.scaffold.share.integration.mes.base.provider.BomRequirementPort;
import com.scaffold.share.integration.mes.base.provider.StepAppearancePort;
import com.scaffold.share.integration.mes.warehouse.provider.LinesideStockPort;
import com.scaffold.share.integration.mes.base.response.BomRequirementLine;

import lombok.RequiredArgsConstructor;

/**
 * WoMaterialRequirementApplicationService 工单物料需求
 * <p>
 * 下达时按 BOM 展开。步骤序号为 0 时，需求量再乘上该站点在 Route 里出现的次数。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class WoMaterialRequirementApplicationService {

    private static final Set<String> COUNTABLE = Set.of("PER_WAFER", "PER_DIE", "PER_STRIP", "PER_UNIT");

    private final BomRequirementPort bom;
    private final StepAppearancePort appearances;
    private final LinesideStockPort stock;
    private final WoMaterialRequirementRepository repository;

    /**
     * 按工单上的 BOM 版本写下需求。
     *
     * @param workOrder 正在下达的工单
     * @return 线边不够的物料提示。不阻止下达
     */
    public List<String> expand(WorkOrder workOrder) {
        List<String> warnings = new ArrayList<>();
        if (workOrder.getBomVersionId() == null) {
            return warnings;
        }
        for (BomRequirementLine line : bom.list(workOrder.getBomVersionId())) {
            WoMaterialRequirement row = new WoMaterialRequirement();
            row.setWoId(workOrder.getId());
            row.setBomItemId(line.bomItemId());
            row.setMaterialId(line.materialId());
            row.setConsumeMode(line.consumeMode());
            BigDecimal need = required(workOrder, line);
            row.setRequiredQty(need);
            repository.save(row);
            if (need != null && stock.available(line.materialId()).compareTo(need) < 0) {
                warnings.add("物料 " + line.materialId());
            }
        }
        return warnings;
    }

    private BigDecimal required(WorkOrder workOrder, BomRequirementLine line) {
        if (!COUNTABLE.contains(line.consumeMode()) || workOrder.getPlanQty() == null || line.qtyPer() == null) {
            return null;
        }
        BigDecimal rate = line.scrapRate() == null ? BigDecimal.ZERO : line.scrapRate().movePointLeft(2);
        BigDecimal qty = BigDecimal.valueOf(workOrder.getPlanQty()).multiply(line.qtyPer()).multiply(BigDecimal.ONE.add(rate));
        if (line.routeSeqNo() != null && line.routeSeqNo() == 0) {
            int times = appearances.count(workOrder.getRouteVersionId(), line.stepId());
            if (times > 1) {
                qty = qty.multiply(BigDecimal.valueOf(times));
            }
        }
        return qty;
    }
}
