package com.scaffold.mes.exec.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.CarrierCreateRequest;
import com.scaffold.mes.exec.domain.entity.Carrier;
import com.scaffold.mes.exec.domain.service.CarrierApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * CarrierControllerV1 载具
 * <p>
 * 新建、绑定和解绑。一个载具同时只绑一个 Lot。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/carriers")
@RequiredArgsConstructor
public class CarrierControllerV1 {

    private final CarrierApplicationService service;

    /**
     * 新建载具。
     *
     * @param request 编码和类型
     * @return 固定为 true
     */
    /**
     * 列出全部载具。
     *
     * @return 按编码排列
     */
    @GetMapping
    public Result<List<Carrier>> list() {
        return Result.success(service.list());
    }

    @PostMapping
    @AuditLog(operation = "新建载具")
    public Result<Boolean> create(@Valid @RequestBody CarrierCreateRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 绑定到 Lot。
     *
     * @param carrierId 载具
     * @param lotId Lot
     * @return 固定为 true
     */
    @PostMapping("/{carrierId}/bind/{lotId}")
    @AuditLog(operation = "绑定载具")
    public Result<Boolean> bind(@PathVariable Long carrierId, @PathVariable Long lotId) {
        service.bind(carrierId, lotId);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 解绑。
     *
     * @param carrierId 载具
     * @return 固定为 true
     */
    @PostMapping("/{carrierId}/unbind")
    @AuditLog(operation = "解绑载具")
    public Result<Boolean> unbind(@PathVariable Long carrierId) {
        service.unbind(carrierId);
        return Result.success(Boolean.TRUE);
    }
}
