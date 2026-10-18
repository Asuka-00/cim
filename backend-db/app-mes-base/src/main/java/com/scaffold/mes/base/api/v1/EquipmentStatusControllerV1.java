package com.scaffold.mes.base.api.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.dto.EquipmentStatusChangeRequest;
import com.scaffold.mes.base.domain.entity.EquipmentStatusHist;
import com.scaffold.mes.base.domain.service.EquipmentStatusApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * EquipmentStatusControllerV1 设备状态接口
 * <p>
 * 提供手工切换和状态历史查询。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/equipment/{equipmentId}")
@RequiredArgsConstructor
public class EquipmentStatusControllerV1 {

    private final EquipmentStatusApplicationService service;

    /**
     * 分页查询状态历史。
     *
     * @param equipmentId 设备
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @return 历史时间段
     */
    @GetMapping("/status-history")
    public Result<PageDTO<EquipmentStatusHist>> history(@PathVariable Long equipmentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(service.history(equipmentId, page, pageSize));
    }

    /**
     * 手工切换设备状态。
     *
     * @param equipmentId 设备
     * @param request 目标状态
     * @return 固定为 true
     */
    @PostMapping("/status")
    @AuditLog(operation = "切换设备状态")
    public Result<Boolean> change(@PathVariable Long equipmentId,
            @Valid @RequestBody EquipmentStatusChangeRequest request) {
        service.change(equipmentId, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 手工切换腔体状态。历史记在该腔体上，不改设备级状态。
     *
     * @param equipmentId 设备
     * @param portId 腔体
     * @param request 目标状态
     * @return 固定为 true
     */
    @PostMapping("/ports/{portId}/status")
    @AuditLog(operation = "切换腔体状态")
    public Result<Boolean> changeChamber(@PathVariable Long equipmentId, @PathVariable Long portId,
            @Valid @RequestBody EquipmentStatusChangeRequest request) {
        service.changeChamber(equipmentId, portId, request);
        return Result.success(Boolean.TRUE);
    }
}
