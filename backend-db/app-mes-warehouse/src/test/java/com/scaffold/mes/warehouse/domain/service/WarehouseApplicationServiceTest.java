package com.scaffold.mes.warehouse.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.warehouse.domain.MesWarehouseErrorCodes;
import com.scaffold.mes.warehouse.domain.dto.LocationCreateRequest;
import com.scaffold.mes.warehouse.domain.dto.SupplierCreateRequest;
import com.scaffold.mes.warehouse.domain.dto.WarehouseCreateRequest;
import com.scaffold.mes.warehouse.domain.entity.StorageLocation;
import com.scaffold.mes.warehouse.domain.entity.Supplier;
import com.scaffold.mes.warehouse.domain.entity.Warehouse;
import com.scaffold.mes.warehouse.domain.repository.WarehouseStore;
import com.scaffold.share.exception.BusinessException;

/**
 * 库位只能建在已启用的库房下。
 */
class WarehouseApplicationServiceTest {

    private WarehouseStore store;
    private WarehouseApplicationService service;

    @BeforeEach
    void setUp() {
        store = mock(WarehouseStore.class);
        when(store.findWarehouseByCode("RAW-1")).thenReturn(Optional.empty());
        Warehouse warehouse = new Warehouse();
        warehouse.setId(3L);
        warehouse.setEnabled(1);
        when(store.findWarehouseById(3L)).thenReturn(Optional.of(warehouse));
        when(store.findLocation(3L, "A-01")).thenReturn(Optional.empty());
        when(store.findSupplierByCode("SUP-1")).thenReturn(Optional.empty());
        service = new WarehouseApplicationService(store);
    }

    @Test
    void createLocationUnderEnabledWarehouse() {
        LocationCreateRequest request = new LocationCreateRequest();
        request.setWhId(3L);
        request.setLocCode("A-01");
        request.setLocName("原料架");

        service.createLocation(request);

        verify(store).saveLocation(org.mockito.ArgumentMatchers.<StorageLocation>argThat(row ->
                row.getWhId().equals(3L) && "A-01".equals(row.getLocCode()) && row.getEnabled().equals(1)));
    }

    @Test
    void disabledWarehouseRejectsLocation() {
        Warehouse warehouse = new Warehouse();
        warehouse.setEnabled(0);
        when(store.findWarehouseById(3L)).thenReturn(Optional.of(warehouse));
        LocationCreateRequest request = new LocationCreateRequest();
        request.setWhId(3L);
        request.setLocCode("A-01");

        assertThatThrownBy(() -> service.createLocation(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesWarehouseErrorCodes.LOCATION_INVALID);
    }

    @Test
    void createWarehouseDefaultsToRaw() {
        WarehouseCreateRequest request = new WarehouseCreateRequest();
        request.setWhCode("RAW-1");
        request.setWhName("原料库");

        service.createWarehouse(request);

        verify(store).saveWarehouse(org.mockito.ArgumentMatchers.<Warehouse>argThat(row ->
                "RAW".equals(row.getWhType()) && row.getEnabled().equals(1)));
    }

    @Test
    void createSupplierAndRejectDuplicate() {
        SupplierCreateRequest request = new SupplierCreateRequest();
        request.setSupplierCode("SUP-1");
        request.setSupplierName("甲供应商");
        request.setContact("王工");

        service.createSupplier(request);

        verify(store).saveSupplier(org.mockito.ArgumentMatchers.<Supplier>argThat(row ->
                "SUP-1".equals(row.getSupplierCode()) && row.getEnabled().equals(1)));
        when(store.findSupplierByCode("SUP-1")).thenReturn(Optional.of(new Supplier()));
        assertThatThrownBy(() -> service.createSupplier(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesWarehouseErrorCodes.SUPPLIER_EXISTS);
    }
}
