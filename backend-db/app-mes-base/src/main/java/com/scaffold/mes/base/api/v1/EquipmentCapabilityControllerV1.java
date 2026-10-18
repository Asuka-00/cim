package com.scaffold.mes.base.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.base.domain.dto.CapabilityDecision;
import com.scaffold.mes.base.domain.dto.EquipmentCapabilitySaveRequest;
import com.scaffold.mes.base.domain.entity.EquipmentCapability;
import com.scaffold.mes.base.domain.service.EquipmentCapabilityApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * EquipmentCapabilityControllerV1 设备能力接口
 * <p>
 * 提供能力列表、判定、新增、修改和删除。判定在没有记录时看站点设备组是否已绑定。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/equipment-capabilities")
@RequiredArgsConstructor
public class EquipmentCapabilityControllerV1 {

    private final EquipmentCapabilityApplicationService service;

    /**
     * 判定设备能否加工。
     *
     * @param equipmentId 设备
     * @param stepId 站点
     * @param recipeId Recipe，可空
     * @param productId 产品，可空
     * @param groupBound 设备组是否已绑该站点
     * @return 判定结果
     */
    /**
     * 列出某台设备的能力。
     *
     * @param equipmentId 设备
     * @return 能力列表
     */
    @GetMapping
    public Result<List<EquipmentCapability>> list(@RequestParam Long equipmentId) {
        return Result.success(service.list(equipmentId));
    }

    @GetMapping("/decide")
    public Result<CapabilityDecision> decide(@RequestParam Long equipmentId, @RequestParam Long stepId,
            @RequestParam(required = false) Long recipeId, @RequestParam(required = false) Long productId,
            @RequestParam(defaultValue = "false") boolean groupBound) {
        return Result.success(service.decide(equipmentId, stepId, recipeId, productId, groupBound));
    }

    /**
     * 新增能力。
     *
     * @param request 能力内容
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新增设备能力")
    public Result<Boolean> create(@Valid @RequestBody EquipmentCapabilitySaveRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 修改能力。
     *
     * @param id 主键
     * @param request 能力内容
     * @return 固定为 true
     */
    @PutMapping("/{id}")
    @AuditLog(operation = "修改设备能力")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody EquipmentCapabilitySaveRequest request) {
        service.update(id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 删除能力。
     *
     * @param id 主键
     * @return 固定为 true
     */
    @DeleteMapping("/{id}")
    @AuditLog(operation = "删除设备能力")
    public Result<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success(Boolean.TRUE);
    }
}
