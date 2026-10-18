package com.scaffold.mes.exec.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.WaferMapImportRequest;
import com.scaffold.mes.exec.domain.entity.WaferMap;
import com.scaffold.mes.exec.domain.service.WaferMapApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * WaferMapControllerV1 导入 Wafer Map
 * <p>
 * 导入后该片改为逐片 Bin，并清掉 Lot 汇总行。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}/wafer-maps")
@RequiredArgsConstructor
public class WaferMapControllerV1 {

    private final WaferMapApplicationService service;

    /**
     * 导入一版 Map。
     *
     * @param lotId Lot
     * @param request Wafer 和 CSV
     * @return 新版本号
     */
    @PostMapping
    @AuditLog(operation = "导入 Wafer Map")
    public Result<Integer> importMap(@PathVariable Long lotId, @Valid @RequestBody WaferMapImportRequest request) {
        return Result.success(service.importMap(lotId, request));
    }

    /**
     * 列出某个 Lot 的全部版本。
     *
     * @param lotId Lot
     * @return 按片和版本排列
     */
    @GetMapping
    public Result<List<WaferMap>> list(@PathVariable Long lotId) {
        return Result.success(service.list(lotId));
    }
}
