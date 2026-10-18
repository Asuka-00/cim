package com.scaffold.mes.warehouse.domain.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.warehouse.domain.MesWarehouseErrorCodes;
import com.scaffold.mes.warehouse.domain.entity.EquipmentMaterialBinding;
import com.scaffold.mes.warehouse.infra.mapper.db.EquipmentMaterialBindingMapper;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;
import com.scaffold.share.integration.mes.warehouse.response.BindingDeduction;

import lombok.RequiredArgsConstructor;

/**
 * EquipmentBindingApplicationService 设备物料绑定扣减
 * <p>
 * 找这台设备上同物料、剩余量够扣的 ACTIVE 绑定。扣到 0 时改为 DEPLETED。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class EquipmentBindingApplicationService {

    private final EquipmentMaterialBindingMapper bindings;
    private final LinesideStockApplicationService stock;

    /**
     * 列出设备物料绑定。
     *
     * @return 全部绑定，没有时为空列表
     */
    public List<EquipmentMaterialBinding> list() {
        return bindings.selectList(new LambdaQueryWrapper<>());
    }

    /**
     * 从绑定剩余量扣减。
     *
     * @param equipmentId 设备
     * @param materialId 物料
     * @param qty 数量
     * @return 扣到的绑定。没有或不够时为空
     */
    @Transactional
    public BindingDeduction deduct(Long equipmentId, Long materialId, BigDecimal qty) {
        if (equipmentId == null || materialId == null || qty == null || qty.signum() <= 0) {
            return null;
        }
        EquipmentMaterialBinding hit = bindings.selectList(new LambdaQueryWrapper<EquipmentMaterialBinding>()
                        .eq(EquipmentMaterialBinding::getEquipmentId, equipmentId)
                        .eq(EquipmentMaterialBinding::getMaterialId, materialId)
                        .eq(EquipmentMaterialBinding::getStatus, "ACTIVE"))
                .stream()
                .filter(row -> row.getRemainingQty() != null && row.getRemainingQty().compareTo(qty) >= 0)
                .min(Comparator.comparing(EquipmentMaterialBinding::getId, Comparator.nullsLast(Long::compareTo)))
                .orElse(null);
        if (hit == null) {
            return null;
        }
        BigDecimal left = hit.getRemainingQty().subtract(qty);
        hit.setRemainingQty(left);
        if (left.signum() == 0) {
            hit.setStatus("DEPLETED");
        }
        new DbRepositorySupport<>(bindings, EquipmentMaterialBinding.class).update(hit);
        return new BindingDeduction(hit.getId(), hit.getMaterialLotId(), hit.getUomId());
    }

    /**
     * 下料。退回线边时把剩余量加回指定库位。报废时只记下剩余量。
     *
     * @param id 绑定
     * @param disposition RETURN_LINESIDE 或 SCRAP
     * @param locationId 退回的线边库位。报废时可空
     * @throws BusinessException 已下料、处置不合法，或退回没写库位
     */
    @Transactional
    public void unmount(Long id, String disposition, Long locationId) {
        EquipmentMaterialBinding binding = bindings.selectById(id);
        if (binding == null || "UNMOUNTED".equals(binding.getStatus())) {
            throw new BusinessException(MesWarehouseErrorCodes.BINDING_UNMOUNT, "绑定已下料");
        }
        if (!"RETURN_LINESIDE".equals(disposition) && !"SCRAP".equals(disposition)) {
            throw new BusinessException(MesWarehouseErrorCodes.BINDING_UNMOUNT, "下料处置不合法");
        }
        BigDecimal remaining = binding.getRemainingQty() == null ? BigDecimal.ZERO : binding.getRemainingQty();
        if ("RETURN_LINESIDE".equals(disposition)) {
            if (locationId == null) {
                throw new BusinessException(MesWarehouseErrorCodes.BINDING_UNMOUNT, "退回线边必须指定库位");
            }
            if (remaining.signum() > 0) {
                stock.restore(binding.getMaterialId(), binding.getMaterialLotId(), locationId, remaining);
            }
        }
        binding.setUnmountedRemainingQty(remaining);
        binding.setUnmountDisposition(disposition);
        binding.setRemainingQty(BigDecimal.ZERO);
        binding.setStatus("UNMOUNTED");
        new DbRepositorySupport<>(bindings, EquipmentMaterialBinding.class).update(binding);
    }

    /**
     * 绑定是否已按报废下料。
     *
     * @param bindingId 绑定
     * @return 状态已下料且处置是 SCRAP 时为 true
     */
    public boolean scrapped(Long bindingId) {
        if (bindingId == null || bindingId == 0L) {
            return false;
        }
        EquipmentMaterialBinding binding = bindings.selectById(bindingId);
        return binding != null && "UNMOUNTED".equals(binding.getStatus()) && "SCRAP".equals(binding.getUnmountDisposition());
    }
}
