package com.scaffold.mes.exec.api.v1;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.LotUdaPutRequest;
import com.scaffold.mes.exec.domain.service.LotUdaApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * LotUdaControllerV1 Lot 自定义属性
 * <p>
 * 写入一个已定义的属性值，读出已写入的值，并查出还缺哪些必填属性。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{id}/uda")
@RequiredArgsConstructor
public class LotUdaControllerV1 {

    private final LotUdaApplicationService service;

    /**
     * 写入一个属性值。
     *
     * @param id Lot
     * @param request 属性编码和值
     * @return 固定为 true
     */
    @PutMapping
    @AuditLog(operation = "写入 Lot 自定义属性")
    public Result<Boolean> put(@PathVariable Long id, @Valid @RequestBody LotUdaPutRequest request) {
        service.put(id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 读出已写入的属性。
     *
     * @param id Lot
     * @return 属性编码到值
     */
    @GetMapping
    public Result<Map<String, String>> show(@PathVariable Long id) {
        return Result.success(service.show(id));
    }

    /**
     * 查出还没写的必填属性。
     *
     * @param id Lot
     * @return 缺失的属性编码
     */
    @GetMapping("/missing")
    public Result<List<String>> missing(@PathVariable Long id) {
        return Result.success(service.missing(id));
    }
}
