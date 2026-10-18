package com.scaffold.mes.base.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.base.domain.dto.EquipmentFaultCloseRequest;
import com.scaffold.mes.base.domain.dto.EquipmentFaultOpenRequest;
import com.scaffold.mes.base.domain.entity.EquipmentFault;
import com.scaffold.mes.base.domain.service.EquipmentFaultApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * EquipmentFaultControllerV1 设备异常
 * <p>
 * 登记和关闭。不自动改设备状态。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/equipment-faults")
@RequiredArgsConstructor
public class EquipmentFaultControllerV1 {

    private final EquipmentFaultApplicationService service;

    /**
     * 登记异常。
     *
     * @param request 单号、设备和描述
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "登记设备异常")
    public Result<Boolean> open(@Valid @RequestBody EquipmentFaultOpenRequest request) {
        service.open(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 关闭异常。
     *
     * @param faultId 异常
     * @param request 处理结果
     * @return 固定为 true
     */
    @PostMapping("/{faultId}/close")
    @AuditLog(operation = "关闭设备异常")
    public Result<Boolean> close(@PathVariable Long faultId, @Valid @RequestBody EquipmentFaultCloseRequest request) {
        service.close(faultId, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 列出全部异常。
     *
     * @return 按单号排列
     */
    @GetMapping
    public Result<List<EquipmentFault>> list() {
        return Result.success(service.list());
    }
}
