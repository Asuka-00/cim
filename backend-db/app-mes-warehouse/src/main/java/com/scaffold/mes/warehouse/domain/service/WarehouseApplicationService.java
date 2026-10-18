package com.scaffold.mes.warehouse.domain.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.warehouse.domain.MesWarehouseErrorCodes;
import com.scaffold.mes.warehouse.domain.dto.LocationCreateRequest;
import com.scaffold.mes.warehouse.domain.dto.SupplierCreateRequest;
import com.scaffold.mes.warehouse.domain.dto.WarehouseCreateRequest;
import com.scaffold.mes.warehouse.domain.entity.StorageLocation;
import com.scaffold.mes.warehouse.domain.entity.Supplier;
import com.scaffold.mes.warehouse.domain.entity.Warehouse;
import com.scaffold.mes.warehouse.domain.repository.WarehouseStore;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * WarehouseApplicationService 库房和库位
 * <p>
 * 库位只能建在已启用的库房下。编码在库房内唯一。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class WarehouseApplicationService {

    private static final Set<String> TYPES = Set.of("RAW", "LINESIDE", "QUARANTINE", "OTHER");

    private final WarehouseStore store;

    /**
     * 新建库房。
     *
     * @param request 编码、名称和类型
     * @throws BusinessException 编码重复或类型不合法
     */
    @Transactional
    public void createWarehouse(WarehouseCreateRequest request) {
        String type = request.getWhType() == null || request.getWhType().isBlank() ? "RAW" : request.getWhType();
        if (!TYPES.contains(type)) {
            throw new BusinessException(MesWarehouseErrorCodes.WAREHOUSE_INVALID, "库房类型不合法");
        }
        if (store.findWarehouseByCode(request.getWhCode()).isPresent()) {
            throw new BusinessException(MesWarehouseErrorCodes.WAREHOUSE_INVALID, "库房编码已存在");
        }
        Warehouse warehouse = new Warehouse();
        warehouse.setWhCode(request.getWhCode());
        warehouse.setWhName(request.getWhName());
        warehouse.setWhType(type);
        warehouse.setEnabled(1);
        store.saveWarehouse(warehouse);
    }

    /**
     * 新建库位。
     *
     * @param request 库房和编码
     * @throws BusinessException 库房未启用，或编码重复
     */
    @Transactional
    public void createLocation(LocationCreateRequest request) {
        Warehouse warehouse = store.findWarehouseById(request.getWhId())
                .orElseThrow(() -> new BusinessException(MesWarehouseErrorCodes.LOCATION_INVALID, "库房不存在"));
        if (warehouse.getEnabled() == null || warehouse.getEnabled() != 1) {
            throw new BusinessException(MesWarehouseErrorCodes.LOCATION_INVALID, "库房未启用");
        }
        if (store.findLocation(request.getWhId(), request.getLocCode()).isPresent()) {
            throw new BusinessException(MesWarehouseErrorCodes.LOCATION_INVALID, "库位编码已存在");
        }
        StorageLocation location = new StorageLocation();
        location.setWhId(request.getWhId());
        location.setLocCode(request.getLocCode());
        location.setLocName(request.getLocName());
        location.setEnabled(1);
        store.saveLocation(location);
    }

    /**
     * 列出库房。
     *
     * @return 按编码排列
     */
    public List<Warehouse> listWarehouses() {
        return store.listWarehouses();
    }

    /**
     * 列出库位。
     *
     * @return 按编码排列
     */
    public List<StorageLocation> listLocations() {
        return store.listLocations();
    }

    /**
     * 新建供应商。
     *
     * @param request 编码和名称
     * @throws BusinessException 编码已存在
     */
    @Transactional
    public void createSupplier(SupplierCreateRequest request) {
        if (store.findSupplierByCode(request.getSupplierCode()).isPresent()) {
            throw new BusinessException(MesWarehouseErrorCodes.SUPPLIER_EXISTS, "供应商编码已存在");
        }
        Supplier supplier = new Supplier();
        supplier.setSupplierCode(request.getSupplierCode());
        supplier.setSupplierName(request.getSupplierName());
        supplier.setContact(request.getContact());
        supplier.setPhone(request.getPhone());
        supplier.setEnabled(1);
        store.saveSupplier(supplier);
    }

    /**
     * 列出供应商。
     *
     * @return 按编码排列
     */
    public List<Supplier> listSuppliers() {
        return store.listSuppliers();
    }
}
