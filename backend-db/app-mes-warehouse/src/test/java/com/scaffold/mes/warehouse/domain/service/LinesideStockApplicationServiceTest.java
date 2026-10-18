package com.scaffold.mes.warehouse.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.warehouse.domain.MesWarehouseErrorCodes;
import com.scaffold.mes.warehouse.domain.entity.Inventory;
import com.scaffold.mes.warehouse.domain.entity.StorageLocation;
import com.scaffold.mes.warehouse.domain.entity.Warehouse;
import com.scaffold.mes.warehouse.infra.mapper.db.InventoryMapper;
import com.scaffold.mes.warehouse.infra.mapper.db.InventoryTxnMapper;
import com.scaffold.mes.warehouse.infra.mapper.db.StorageLocationMapper;
import com.scaffold.mes.warehouse.infra.mapper.db.WarehouseMapper;
import com.scaffold.share.exception.BusinessException;

/**
 * 从最早的一笔够扣的库存里减。可用量不够时拒绝。
 */
class LinesideStockApplicationServiceTest {

    private InventoryMapper inventories;
    private LinesideStockApplicationService service;

    @BeforeEach
    void setUp() {
        inventories = mock(InventoryMapper.class);
        StorageLocationMapper locations = mock(StorageLocationMapper.class);
        WarehouseMapper warehouses = mock(WarehouseMapper.class);
        when(locations.selectById(3L)).thenReturn(location(3L, 2L));
        when(warehouses.selectById(2L)).thenReturn(warehouse("LINESIDE"));
        when(locations.selectById(9L)).thenReturn(location(9L, 1L));
        when(warehouses.selectById(1L)).thenReturn(warehouse("RAW"));
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T08:00:00Z"), ZoneOffset.ofHours(8));
        service = new LinesideStockApplicationService(inventories, mock(InventoryTxnMapper.class), locations, warehouses,
                clock);
    }

    @Test
    void deductsFirstEnoughRow() {
        Inventory shortRow = row(1L, "1", "0");
        Inventory enough = row(2L, "5", "1");
        when(inventories.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(enough, shortRow));

        service.deduct(15L, new BigDecimal("1.5"));

        assertThat(enough.getQtyOnHand()).isEqualByComparingTo("3.5");
        assertThat(shortRow.getQtyOnHand()).isEqualByComparingTo("1");
    }

    @Test
    void rawWarehouseIsSkipped() {
        Inventory raw = row(1L, "10", "0");
        raw.setLocationId(9L);
        Inventory line = row(2L, "5", "1");
        when(inventories.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(raw, line));

        service.deduct(15L, new BigDecimal("1.5"));

        assertThat(raw.getQtyOnHand()).isEqualByComparingTo("10");
        assertThat(line.getQtyOnHand()).isEqualByComparingTo("3.5");
    }

    @Test
    void restoreAddsQtyBack() {
        Inventory line = row(2L, "3.5", "1");
        line.setMaterialLotId(20L);
        when(inventories.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(line));

        service.restore(15L, 20L, 3L, new BigDecimal("1.5"));

        assertThat(line.getQtyOnHand()).isEqualByComparingTo("5");
    }

    @Test
    void shortStockReturnsEmpty() {
        Inventory row = row(1L, "1", "0");
        when(inventories.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(row));

        assertThat(service.deduct(15L, new BigDecimal("2"))).isNull();
        assertThat(row.getQtyOnHand()).isEqualByComparingTo("1");
    }

    private static Inventory row(Long id, String onHand, String frozen) {
        Inventory row = new Inventory();
        row.setId(id);
        row.setMaterialId(15L);
        row.setMaterialLotId(20L);
        row.setLocationId(3L);
        row.setQtyOnHand(new BigDecimal(onHand));
        row.setQtyFrozen(new BigDecimal(frozen));
        return row;
    }

    private static StorageLocation location(Long id, Long whId) {
        StorageLocation location = new StorageLocation();
        location.setId(id);
        location.setWhId(whId);
        return location;
    }

    private static Warehouse warehouse(String type) {
        Warehouse warehouse = new Warehouse();
        warehouse.setWhType(type);
        return warehouse;
    }
}
