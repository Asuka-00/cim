package com.scaffold.mes.warehouse.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.warehouse.domain.dto.BindingUnmountRequest;
import com.scaffold.mes.warehouse.domain.entity.EquipmentMaterialBinding;
import com.scaffold.mes.warehouse.domain.service.EquipmentBindingApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * EquipmentBindingControllerV1 设备物料下料
 * <p>
 * 剩余量可以退回线边，或记成报废。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/equipment-bindings")
@RequiredArgsConstructor
public class EquipmentBindingControllerV1 {

    private final EquipmentBindingApplicationService service;

    /**
     * 列出绑定。
     *
     * @return 全部绑定
     */
    @GetMapping
    public Result<List<EquipmentMaterialBinding>> list() {
        return Result.success(service.list());
    }

    /**
     * 下料。
     *
     * @param id 绑定
     * @param request 处置和库位
     * @return 固定为 true
     */
    @PostMapping("/{id}/unmount")
    @AuditLog(operation = "设备物料下料")
    public Result<Boolean> unmount(@PathVariable Long id, @Valid @RequestBody BindingUnmountRequest request) {
        service.unmount(id, request.getDisposition(), request.getLocationId());
        return Result.success(Boolean.TRUE);
    }
}
