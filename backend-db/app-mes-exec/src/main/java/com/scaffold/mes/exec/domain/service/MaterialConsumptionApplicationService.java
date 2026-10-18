package com.scaffold.mes.exec.domain.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.MaterialConsumption;
import com.scaffold.mes.exec.domain.repository.MaterialConsumptionRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.warehouse.provider.EquipmentBindingPort;
import com.scaffold.share.integration.mes.warehouse.provider.LinesideStockPort;
import com.scaffold.share.integration.mes.warehouse.response.BindingDeduction;
import com.scaffold.share.integration.mes.warehouse.response.StockDeduction;

import lombok.RequiredArgsConstructor;

/**
 * MaterialConsumptionApplicationService 物料消耗
 * <p>
 * 出站扣到线边库存后追加一条 BACKFLUSH 记录。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class MaterialConsumptionApplicationService {

    private final MaterialConsumptionRepository repository;
    private final LinesideStockPort stock;
    private final EquipmentBindingPort bindings;
    private final Clock clock;

    /**
     * 记下一次按批反冲。
     *
     * @param lot 正在出站的 Lot
     * @param materialId 物料
     * @param qty 数量
     * @param consumeMode 消耗方式
     * @param taken 被扣的库存。欠账时物料批和库位为 0
     * @param shortage 线边不够、未扣库存时为 true
     */
    public void record(Lot lot, Long materialId, BigDecimal qty, String consumeMode, StockDeduction taken,
            boolean shortage) {
        MaterialConsumption row = new MaterialConsumption();
        row.setLotId(lot.getId());
        row.setStepId(lot.getStepId());
        row.setMaterialId(materialId);
        row.setMaterialLotId(taken.materialLotId() == null ? 0L : taken.materialLotId());
        row.setLocationId(taken.locationId() == null ? 0L : taken.locationId());
        row.setQty(qty);
        row.setUomId(taken.uomId() == null ? 0L : taken.uomId());
        row.setConsumeMode(consumeMode);
        row.setSource("BACKFLUSH");
        row.setIsShortage(shortage ? 1 : 0);
        row.setOccurredAt(LocalDateTime.now(clock));
        repository.save(row);
    }

    /**
     * 记下一次从设备绑定扣的反冲。库位为 0。
     *
     * @param lot 正在出站的 Lot
     * @param materialId 物料
     * @param qty 数量
     * @param consumeMode 消耗方式
     * @param bound 被扣的绑定
     */
    public void recordBound(Lot lot, Long materialId, BigDecimal qty, String consumeMode, BindingDeduction bound) {
        MaterialConsumption row = new MaterialConsumption();
        row.setLotId(lot.getId());
        row.setStepId(lot.getStepId());
        row.setMaterialId(materialId);
        row.setMaterialLotId(bound.materialLotId() == null ? 0L : bound.materialLotId());
        row.setLocationId(0L);
        row.setBindingId(bound.bindingId());
        row.setQty(qty);
        row.setUomId(bound.uomId() == null ? 0L : bound.uomId());
        row.setConsumeMode(consumeMode);
        row.setSource("BACKFLUSH");
        row.setIsShortage(0);
        row.setOccurredAt(LocalDateTime.now(clock));
        repository.save(row);
    }

    /**
     * 冲销这个站点上还没冲过的反冲。数量加回原库位，并追加负数记录。
     *
     * @param lot 正在撤销出站的 Lot
     * @param stepId 被撤销的出站站点
     */
    public void reverse(Lot lot, Long stepId) {
        if (stepId == null) {
            return;
        }
        List<MaterialConsumption> rows = repository.listByLot(lot.getId());
        for (MaterialConsumption row : rows) {
            if (!stepId.equals(row.getStepId()) || !"BACKFLUSH".equals(row.getSource()) || row.getQty() == null
                    || row.getQty().signum() <= 0 || Integer.valueOf(1).equals(row.getIsShortage())
                    || alreadyReversed(rows, row)) {
                continue;
            }
            stock.restore(row.getMaterialId(), row.getMaterialLotId(), row.getLocationId(), row.getQty());
            MaterialConsumption undo = new MaterialConsumption();
            undo.setLotId(lot.getId());
            undo.setStepId(stepId);
            undo.setMaterialId(row.getMaterialId());
            undo.setMaterialLotId(row.getMaterialLotId());
            undo.setLocationId(row.getLocationId());
            undo.setQty(row.getQty().negate());
            undo.setUomId(row.getUomId());
            undo.setConsumeMode(row.getConsumeMode());
            undo.setSource("UNDO");
            undo.setOccurredAt(LocalDateTime.now(clock));
            repository.save(undo);
        }
    }

    /**
     * 补扣一笔欠账。先冲销欠账数量，再记下真正扣到的库存。
     *
     * @param id 欠账消耗
     * @throws BusinessException 不是欠账，或线边仍不够
     */
    @Transactional
    public void settle(Long id) {
        MaterialConsumption debt = repository.findById(id)
                .filter(row -> Integer.valueOf(1).equals(row.getIsShortage()))
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.SHORTAGE_UNSETTLED, "这不是未清偿的欠账"));
        if (repository.existsSettle(id)) {
            throw new BusinessException(MesExecErrorCodes.SHORTAGE_UNSETTLED, "这笔欠账已经清偿");
        }
        StockDeduction taken = stock.deduct(debt.getMaterialId(), debt.getQty());
        if (taken == null) {
            throw new BusinessException(MesExecErrorCodes.SHORTAGE_UNSETTLED, "线边仍不够补扣");
        }
        repository.save(copy(debt, debt.getQty().negate(), "SETTLE", new StockDeduction(0L, 0L, debt.getUomId()), debt.getId()));
        repository.save(copy(debt, debt.getQty(), "BACKFLUSH", taken, debt.getId()));
    }

    /**
     * 按修正后的数量追加差额。原消耗不改。
     *
     * @param id 原消耗
     * @param qtyAfter 修正后的数量
     * @throws BusinessException 原记录是欠账、数量不合法，或线边不够再扣
     */
    @Transactional
    public void adjust(Long id, BigDecimal qtyAfter) {
        MaterialConsumption original = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.CONSUMPTION_ADJUST, "消耗记录不存在"));
        if (Integer.valueOf(1).equals(original.getIsShortage()) || qtyAfter == null || qtyAfter.signum() < 0
                || original.getQty() == null || bindings.scrapped(original.getBindingId())) {
            throw new BusinessException(MesExecErrorCodes.CONSUMPTION_ADJUST, "这笔消耗不能修正");
        }
        BigDecimal delta = qtyAfter.subtract(original.getQty());
        if (delta.signum() == 0) {
            return;
        }
        if (delta.signum() > 0) {
            StockDeduction taken = stock.deduct(original.getMaterialId(), delta);
            if (taken == null) {
                throw new BusinessException(MesExecErrorCodes.CONSUMPTION_ADJUST, "线边不够再扣差额");
            }
            repository.save(copy(original, delta, "ADJUST", taken, original.getId()));
            return;
        }
        stock.restore(original.getMaterialId(), original.getMaterialLotId(), original.getLocationId(), delta.negate());
        repository.save(copy(original, delta, "ADJUST",
                new StockDeduction(original.getMaterialLotId(), original.getLocationId(), original.getUomId()),
                original.getId()));
    }

    private MaterialConsumption copy(MaterialConsumption debt, BigDecimal qty, String source, StockDeduction taken,
            Long refId) {
        MaterialConsumption row = new MaterialConsumption();
        row.setLotId(debt.getLotId());
        row.setStepId(debt.getStepId());
        row.setMaterialId(debt.getMaterialId());
        row.setMaterialLotId(taken.materialLotId() == null ? 0L : taken.materialLotId());
        row.setLocationId(taken.locationId() == null ? 0L : taken.locationId());
        row.setQty(qty);
        row.setUomId(taken.uomId() == null ? 0L : taken.uomId());
        row.setConsumeMode(debt.getConsumeMode());
        row.setSource(source);
        row.setIsShortage(0);
        row.setAdjustRefId(refId);
        row.setOccurredAt(LocalDateTime.now(clock));
        return row;
    }

    private static boolean alreadyReversed(List<MaterialConsumption> rows, MaterialConsumption backflush) {
        return rows.stream().anyMatch(row -> "UNDO".equals(row.getSource())
                && backflush.getMaterialId().equals(row.getMaterialId())
                && backflush.getMaterialLotId().equals(row.getMaterialLotId())
                && backflush.getLocationId().equals(row.getLocationId())
                && backflush.getStepId().equals(row.getStepId()));
    }
}
