package com.scaffold.mes.warehouse.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.warehouse.domain.entity.StorageLocation;
import com.scaffold.mes.warehouse.domain.entity.Supplier;
import com.scaffold.mes.warehouse.domain.entity.Warehouse;
import com.scaffold.mes.warehouse.domain.repository.WarehouseStore;
import com.scaffold.mes.warehouse.infra.mapper.db.StorageLocationMapper;
import com.scaffold.mes.warehouse.infra.mapper.db.SupplierMapper;
import com.scaffold.mes.warehouse.infra.mapper.db.WarehouseMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * WarehouseStoreDb 库房和库位实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class WarehouseStoreDb implements WarehouseStore {

    private final WarehouseMapper warehouses;
    private final StorageLocationMapper locations;
    private final SupplierMapper suppliers;

    @Override
    public Optional<Warehouse> findWarehouseByCode(String whCode) {
        return new DbRepositorySupport<>(warehouses, Warehouse.class)
                .findOne(new LambdaQueryWrapper<Warehouse>().eq(Warehouse::getWhCode, whCode));
    }

    @Override
    public Optional<Warehouse> findWarehouseById(Long id) {
        return new DbRepositorySupport<>(warehouses, Warehouse.class).findById(id);
    }

    @Override
    public void saveWarehouse(Warehouse warehouse) {
        new DbRepositorySupport<>(warehouses, Warehouse.class).save(warehouse);
    }

    @Override
    public List<Warehouse> listWarehouses() {
        return warehouses.selectList(new LambdaQueryWrapper<Warehouse>().orderByAsc(Warehouse::getWhCode));
    }

    @Override
    public Optional<StorageLocation> findLocation(Long whId, String locCode) {
        return new DbRepositorySupport<>(locations, StorageLocation.class).findOne(new LambdaQueryWrapper<StorageLocation>()
                .eq(StorageLocation::getWhId, whId)
                .eq(StorageLocation::getLocCode, locCode));
    }

    @Override
    public Optional<StorageLocation> findLocationById(Long id) {
        return new DbRepositorySupport<>(locations, StorageLocation.class).findById(id);
    }

    @Override
    public void saveLocation(StorageLocation location) {
        new DbRepositorySupport<>(locations, StorageLocation.class).save(location);
    }

    @Override
    public List<StorageLocation> listLocations() {
        return locations.selectList(new LambdaQueryWrapper<StorageLocation>().orderByAsc(StorageLocation::getLocCode));
    }

    @Override
    public Optional<Supplier> findSupplierByCode(String supplierCode) {
        return new DbRepositorySupport<>(suppliers, Supplier.class)
                .findOne(new LambdaQueryWrapper<Supplier>().eq(Supplier::getSupplierCode, supplierCode));
    }

    @Override
    public void saveSupplier(Supplier supplier) {
        new DbRepositorySupport<>(suppliers, Supplier.class).save(supplier);
    }

    @Override
    public List<Supplier> listSuppliers() {
        return suppliers.selectList(new LambdaQueryWrapper<Supplier>().orderByAsc(Supplier::getSupplierCode));
    }
}
