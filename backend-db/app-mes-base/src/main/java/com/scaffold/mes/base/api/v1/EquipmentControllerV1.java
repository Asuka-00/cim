package com.scaffold.mes.base.api.v1;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.dto.EquipmentSaveRequest;
import com.scaffold.mes.base.domain.entity.Equipment;
import com.scaffold.mes.base.domain.service.EquipmentApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * EquipmentControllerV1 设备台账接口
 * <p>
 * 提供设备的查询、新增、修改和删除。状态切换不在本接口。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/equipment")
@RequiredArgsConstructor
public class EquipmentControllerV1 {

    private final EquipmentApplicationService service;

    /**
     * 分页查询设备。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param equipmentCode 编码模糊条件，可空
     * @return 分页结果
     */
    @GetMapping
    public Result<PageDTO<Equipment>> page(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String equipmentCode) {
        return Result.success(service.page(page, pageSize, equipmentCode));
    }

    /**
     * 新增设备。
     *
     * @param request 台账内容
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新增设备")
    public Result<Boolean> create(@Valid @RequestBody EquipmentSaveRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 修改设备台账。
     *
     * @param id 主键
     * @param request 台账内容
     * @return 固定为 true
     */
    @PutMapping("/{id}")
    @AuditLog(operation = "修改设备")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody EquipmentSaveRequest request) {
        service.update(id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 删除设备。
     *
     * @param id 主键
     * @return 固定为 true
     */
    @DeleteMapping("/{id}")
    @AuditLog(operation = "删除设备")
    public Result<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success(Boolean.TRUE);
    }
}
