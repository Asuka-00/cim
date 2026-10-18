package com.scaffold.mes.exec.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.LotConsumptionResponse;
import com.scaffold.mes.exec.domain.service.LotConsumptionQueryApplicationService;
import com.scaffold.share.vo.Result;

import lombok.RequiredArgsConstructor;

/**
 * LotConsumptionControllerV1 物料消耗追溯
 * <p>
 * 按 Lot 列出它消耗过的物料。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots")
@RequiredArgsConstructor
public class LotConsumptionControllerV1 {

    private final LotConsumptionQueryApplicationService service;

    /**
     * 列出消耗。
     *
     * @param id Lot
     * @return 消耗流水
     */
    @GetMapping("/{id}/consumptions")
    public Result<List<LotConsumptionResponse>> list(@PathVariable Long id) {
        return Result.success(service.list(id));
    }
}
