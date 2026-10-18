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

import com.scaffold.mes.base.domain.dto.StepEquipmentGroupSaveRequest;
import com.scaffold.mes.base.domain.entity.StepEquipmentGroup;
import com.scaffold.mes.base.domain.service.StepEquipmentGroupApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * StepEquipmentGroupControllerV1 站点设备组绑定接口
 * <p>
 * 提供绑定的查询、新增、修改和删除。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/step-equipment-groups")
@RequiredArgsConstructor
public class StepEquipmentGroupControllerV1 {

    private final StepEquipmentGroupApplicationService service;

    /**
     * 列出某个站点的设备组绑定。
     *
     * @param stepId 站点
     * @return 按优先级从小到大的绑定
     */
    @GetMapping
    public Result<List<StepEquipmentGroup>> list(@RequestParam Long stepId) {
        return Result.success(service.list(stepId));
    }

    /**
     * 新增绑定。
     *
     * @param request 绑定内容
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新增站点设备组绑定")
    public Result<Boolean> create(@Valid @RequestBody StepEquipmentGroupSaveRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 修改绑定。
     *
     * @param id 主键
     * @param request 绑定内容
     * @return 固定为 true
     */
    @PutMapping("/{id}")
    @AuditLog(operation = "修改站点设备组绑定")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody StepEquipmentGroupSaveRequest request) {
        service.update(id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 删除绑定。
     *
     * @param id 主键
     * @return 固定为 true
     */
    @DeleteMapping("/{id}")
    @AuditLog(operation = "删除站点设备组绑定")
    public Result<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success(Boolean.TRUE);
    }
}
