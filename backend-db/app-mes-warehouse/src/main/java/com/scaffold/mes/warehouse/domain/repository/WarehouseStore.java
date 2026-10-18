package com.scaffold.mes.warehouse.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.warehouse.domain.entity.StorageLocation;
import com.scaffold.mes.warehouse.domain.entity.Supplier;
import com.scaffold.mes.warehouse.domain.entity.Warehouse;

/**
 * WarehouseStore 库房和库位
 * <p>
 * 收货前用它确认库位已启用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface WarehouseStore {

    /**
     * 按编码查询库房。
     *
     * @param whCode 编码
     * @return 库房，不存在时为空
     */
    Optional<Warehouse> findWarehouseByCode(String whCode);

    /**
     * 按主键查询库房。
     *
     * @param id 库房
     * @return 库房，不存在时为空
     */
    Optional<Warehouse> findWarehouseById(Long id);

    /**
     * 插入库房。
     *
     * @param warehouse 库房
     */
    void saveWarehouse(Warehouse warehouse);

    /**
     * 列出库房。
     *
     * @return 按编码排列，没有时为空列表
     */
    List<Warehouse> listWarehouses();

    /**
     * 按库房和编码查询库位。
     *
     * @param whId 库房
     * @param locCode 库位编码
     * @return 库位，不存在时为空
     */
    Optional<StorageLocation> findLocation(Long whId, String locCode);

    /**
     * 按主键查询库位。
     *
     * @param id 库位
     * @return 库位，不存在时为空
     */
    Optional<StorageLocation> findLocationById(Long id);

    /**
     * 插入库位。
     *
     * @param location 库位
     */
    void saveLocation(StorageLocation location);

    /**
     * 列出库位。
     *
     * @return 按编码排列，没有时为空列表
     */
    List<StorageLocation> listLocations();

    /**
     * 按编码查询供应商。
     *
     * @param supplierCode 编码
     * @return 供应商，不存在时为空
     */
    Optional<Supplier> findSupplierByCode(String supplierCode);

    /**
     * 插入供应商。
     *
     * @param supplier 供应商
     */
    void saveSupplier(Supplier supplier);

    /**
     * 列出供应商。
     *
     * @return 按编码排列，没有时为空列表
     */
    List<Supplier> listSuppliers();
}
