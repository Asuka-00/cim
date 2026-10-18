package com.scaffold.mes.exec.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.WipStepResponse;
import com.scaffold.mes.exec.domain.service.WipByStepApplicationService;
import com.scaffold.share.vo.Result;

import lombok.RequiredArgsConstructor;

/**
 * WipByStepControllerV1 在制分布
 * <p>
 * 按站点统计还在制的 Lot 数和数量。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/reports/wip-by-step")
@RequiredArgsConstructor
public class WipByStepControllerV1 {

    private final WipByStepApplicationService service;

    /**
     * 按站点汇总。
     *
     * @return 每个站点一行
     */
    @GetMapping
    public Result<List<WipStepResponse>> summarize() {
        return Result.success(service.summarize());
    }
}
