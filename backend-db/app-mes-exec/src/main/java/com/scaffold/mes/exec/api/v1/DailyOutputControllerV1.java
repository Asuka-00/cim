package com.scaffold.mes.exec.api.v1;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.DailyOutputResponse;
import com.scaffold.mes.exec.domain.dto.ProductOutputResponse;
import com.scaffold.mes.exec.domain.service.DailyOutputApplicationService;
import com.scaffold.share.vo.Result;

import lombok.RequiredArgsConstructor;

/**
 * DailyOutputControllerV1 生产日报
 * <p>
 * 统计某一天完成的 Lot 数和数量。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/reports/daily-output")
@RequiredArgsConstructor
public class DailyOutputControllerV1 {

    private final DailyOutputApplicationService service;

    /**
     * 汇总这一天。
     *
     * @param date 日期，格式 yyyy-MM-dd
     * @return 批数和数量
     */
    @GetMapping
    public Result<DailyOutputResponse> summarize(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(service.summarize(date));
    }

    /**
     * 按产品汇总这一天。
     *
     * @param date 日期，格式 yyyy-MM-dd
     * @return 每个产品一行
     */
    @GetMapping("/by-product")
    public Result<List<ProductOutputResponse>> byProduct(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(service.byProduct(date));
    }
}
