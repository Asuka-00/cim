package com.scaffold.mes.base.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.base.domain.dto.EquipmentPortSaveRequest;
import com.scaffold.mes.base.domain.entity.EquipmentPort;
import com.scaffold.mes.base.domain.service.EquipmentPortApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * EquipmentPortControllerV1 设备端口接口
 * <p>
 * 提供端口的查询、新增、修改和删除。状态切换不在本接口。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/equipment/{equipmentId}/ports")
@RequiredArgsConstructor
public class EquipmentPortControllerV1 {

    private final EquipmentPortApplicationService service;

    /**
     * 列出端口。
     *
     * @param equipmentId 设备
     * @return 端口列表
     */
    @GetMapping
    public Result<List<EquipmentPort>> list(@PathVariable Long equipmentId) {
        return Result.success(service.list(equipmentId));
    }

    /**
     * 新增端口。
     *
     * @param equipmentId 设备
     * @param request 端口内容
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新增设备端口")
    public Result<Boolean> create(@PathVariable Long equipmentId, @Valid @RequestBody EquipmentPortSaveRequest request) {
        service.create(equipmentId, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 修改端口台账。
     *
     * @param equipmentId 设备
     * @param id 端口主键
     * @param request 端口内容
     * @return 固定为 true
     */
    @PutMapping("/{id}")
    @AuditLog(operation = "修改设备端口")
    public Result<Boolean> update(@PathVariable Long equipmentId, @PathVariable Long id,
            @Valid @RequestBody EquipmentPortSaveRequest request) {
        service.update(equipmentId, id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 删除端口。
     *
     * @param equipmentId 设备
     * @param id 端口主键
     * @return 固定为 true
     */
    @DeleteMapping("/{id}")
    @AuditLog(operation = "删除设备端口")
    public Result<Boolean> delete(@PathVariable Long equipmentId, @PathVariable Long id) {
        service.delete(equipmentId, id);
        return Result.success(Boolean.TRUE);
    }
}
