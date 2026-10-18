package com.scaffold.mes.warehouse.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.warehouse.domain.MesWarehouseErrorCodes;
import com.scaffold.mes.warehouse.domain.entity.EquipmentMaterialBinding;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.mes.warehouse.infra.mapper.db.EquipmentMaterialBindingMapper;
import com.scaffold.share.integration.mes.warehouse.response.BindingDeduction;

/**
 * 从设备上剩余量够扣的绑定减。扣完改为 DEPLETED。不够时为空。
 */
class EquipmentBindingApplicationServiceTest {

    private EquipmentMaterialBindingMapper bindings;
    private LinesideStockApplicationService stock;
    private EquipmentBindingApplicationService service;

    @BeforeEach
    void setUp() {
        bindings = mock(EquipmentMaterialBindingMapper.class);
        stock = mock(LinesideStockApplicationService.class);
        service = new EquipmentBindingApplicationService(bindings, stock);
    }

    @Test
    void deductsRemainingAndDepletesAtZero() {
        EquipmentMaterialBinding row = binding(new BigDecimal("1.5"));
        when(bindings.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(row));

        BindingDeduction taken = service.deduct(3L, 15L, new BigDecimal("1.5"));

        assertThat(taken.bindingId()).isEqualTo(9L);
        assertThat(row.getRemainingQty()).isEqualByComparingTo("0");
        assertThat(row.getStatus()).isEqualTo("DEPLETED");
    }

    @Test
    void shortBindingReturnsEmpty() {
        when(bindings.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(binding(new BigDecimal("1"))));

        assertThat(service.deduct(3L, 15L, new BigDecimal("1.5"))).isNull();
    }

    @Test
    void returnPutsRemainingBack() {
        EquipmentMaterialBinding row = binding(new BigDecimal("0.4"));
        when(bindings.selectById(9L)).thenReturn(row);

        service.unmount(9L, "RETURN_LINESIDE", 3L);

        verify(stock).restore(15L, 20L, 3L, new BigDecimal("0.4"));
        assertThat(row.getStatus()).isEqualTo("UNMOUNTED");
        assertThat(row.getUnmountDisposition()).isEqualTo("RETURN_LINESIDE");
        assertThat(row.getUnmountedRemainingQty()).isEqualByComparingTo("0.4");
        assertThat(row.getRemainingQty()).isEqualByComparingTo("0");
    }

    @Test
    void scrapDoesNotReturnStock() {
        EquipmentMaterialBinding row = binding(new BigDecimal("0.4"));
        when(bindings.selectById(9L)).thenReturn(row);

        service.unmount(9L, "SCRAP", null);

        verify(stock, never()).restore(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThat(row.getUnmountDisposition()).isEqualTo("SCRAP");
        assertThat(row.getStatus()).isEqualTo("UNMOUNTED");
    }

    @Test
    void secondUnmountRejected() {
        EquipmentMaterialBinding row = binding(BigDecimal.ZERO);
        row.setStatus("UNMOUNTED");
        when(bindings.selectById(9L)).thenReturn(row);

        assertThatThrownBy(() -> service.unmount(9L, "SCRAP", null))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesWarehouseErrorCodes.BINDING_UNMOUNT);
    }

    private static EquipmentMaterialBinding binding(BigDecimal remaining) {
        EquipmentMaterialBinding row = new EquipmentMaterialBinding();
        row.setId(9L);
        row.setEquipmentId(3L);
        row.setMaterialId(15L);
        row.setMaterialLotId(20L);
        row.setUomId(8L);
        row.setRemainingQty(remaining);
        row.setStatus("ACTIVE");
        return row;
    }
}
