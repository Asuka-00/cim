package com.scaffold.mes.exec.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.PackContainerCreateRequest;
import com.scaffold.mes.exec.domain.dto.PackRequest;
import com.scaffold.mes.exec.domain.entity.PackContainer;
import com.scaffold.mes.exec.domain.service.PackContainerApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * PackContainerControllerV1 包装容器
 * <p>
 * 新建空容器，并在装满时改为 FULL。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes")
@RequiredArgsConstructor
public class PackContainerControllerV1 {

    private final PackContainerApplicationService service;

    /**
     * 新建容器。
     *
     * @param lotId Lot
     * @param request 容器号、类型和容量
     * @return 固定为 true
     */
    @PostMapping("/lots/{lotId}/containers")
    @AuditLog(operation = "新建包装容器")
    public Result<Boolean> create(@PathVariable Long lotId, @Valid @RequestBody PackContainerCreateRequest request) {
        service.create(lotId, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 装入。
     *
     * @param containerId 容器
     * @param request 本次数量
     * @return 固定为 true
     */
    @PostMapping("/containers/{containerId}/pack")
    @AuditLog(operation = "装入包装容器")
    public Result<Boolean> pack(@PathVariable Long containerId, @Valid @RequestBody PackRequest request) {
        service.pack(containerId, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 列出某个 Lot 的容器。
     *
     * @param lotId Lot
     * @return 按容器号排列
     */
    @GetMapping("/lots/{lotId}/containers")
    public Result<List<PackContainer>> list(@PathVariable Long lotId) {
        return Result.success(service.list(lotId));
    }
}
