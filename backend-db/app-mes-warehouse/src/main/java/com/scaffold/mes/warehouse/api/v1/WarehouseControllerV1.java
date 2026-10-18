package com.scaffold.mes.warehouse.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.warehouse.domain.dto.LocationCreateRequest;
import com.scaffold.mes.warehouse.domain.dto.SupplierCreateRequest;
import com.scaffold.mes.warehouse.domain.dto.WarehouseCreateRequest;
import com.scaffold.mes.warehouse.domain.entity.StorageLocation;
import com.scaffold.mes.warehouse.domain.entity.Supplier;
import com.scaffold.mes.warehouse.domain.entity.Warehouse;
import com.scaffold.mes.warehouse.domain.service.WarehouseApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * WarehouseControllerV1 库房和库位
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes")
@RequiredArgsConstructor
public class WarehouseControllerV1 {

    private final WarehouseApplicationService service;

    /**
     * 新建库房。
     *
     * @param request 编码和名称
     * @return 固定为 true
     */
    @PostMapping("/warehouses")
    @AuditLog(operation = "新建库房")
    public Result<Boolean> createWarehouse(@Valid @RequestBody WarehouseCreateRequest request) {
        service.createWarehouse(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 新建库位。
     *
     * @param request 库房和编码
     * @return 固定为 true
     */
    @PostMapping("/locations")
    @AuditLog(operation = "新建库位")
    public Result<Boolean> createLocation(@Valid @RequestBody LocationCreateRequest request) {
        service.createLocation(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 列出库房。
     *
     * @return 按编码排列
     */
    @GetMapping("/warehouses")
    public Result<List<Warehouse>> warehouses() {
        return Result.success(service.listWarehouses());
    }

    /**
     * 列出库位。
     *
     * @return 按编码排列
     */
    @GetMapping("/locations")
    public Result<List<StorageLocation>> locations() {
        return Result.success(service.listLocations());
    }

    /**
     * 新建供应商。
     *
     * @param request 编码和名称
     * @return 固定为 true
     */
    @PostMapping("/suppliers")
    @AuditLog(operation = "新建供应商")
    public Result<Boolean> createSupplier(@Valid @RequestBody SupplierCreateRequest request) {
        service.createSupplier(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 列出供应商。
     *
     * @return 按编码排列
     */
    @GetMapping("/suppliers")
    public Result<List<Supplier>> suppliers() {
        return Result.success(service.listSuppliers());
    }
}
