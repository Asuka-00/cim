package com.scaffold.mes.warehouse.infra.integration;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.scaffold.mes.warehouse.domain.service.EquipmentBindingApplicationService;
import com.scaffold.share.integration.mes.warehouse.provider.EquipmentBindingPort;
import com.scaffold.share.integration.mes.warehouse.response.BindingDeduction;

import lombok.RequiredArgsConstructor;

/**
 * EquipmentBindingPortImpl 设备物料绑定
 * <p>
 * 转给绑定服务扣剩余量。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class EquipmentBindingPortImpl implements EquipmentBindingPort {

    private final EquipmentBindingApplicationService bindings;

    @Override
    public BindingDeduction deduct(Long equipmentId, Long materialId, BigDecimal qty) {
        return bindings.deduct(equipmentId, materialId, qty);
    }

    @Override
    public boolean scrapped(Long bindingId) {
        return bindings.scrapped(bindingId);
    }
}
