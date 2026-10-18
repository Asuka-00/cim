package com.scaffold.mes.exec.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.UnitSerialRequest;
import com.scaffold.mes.exec.domain.entity.UnitSerial;
import com.scaffold.mes.exec.domain.service.UnitSerialApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * UnitSerialControllerV1 登记 Unit 序列号
 * <p>
 * 产品未打开跟踪时拒绝。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}/unit-serials")
@RequiredArgsConstructor
public class UnitSerialControllerV1 {

    private final UnitSerialApplicationService service;

    /**
     * 登记序列号。
     *
     * @param lotId Lot
     * @param request 序列号
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "登记 Unit 序列号")
    public Result<Boolean> assign(@PathVariable Long lotId, @Valid @RequestBody UnitSerialRequest request) {
        service.assign(lotId, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 列出某个 Lot 的序列号。
     *
     * @param lotId Lot
     * @return 按序列号排列
     */
    @GetMapping
    public Result<List<UnitSerial>> list(@PathVariable Long lotId) {
        return Result.success(service.list(lotId));
    }
}
