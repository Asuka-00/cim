package com.scaffold.system.api.v1.system;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.share.vo.Result;
import com.scaffold.system.domain.service.system.QpsTestApplicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * QPS 测试接口。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-05-29
 */
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
@Tag(name = "QpsTestController", description = "QPS 压测接口")
public class QpsTestControllerV1 {

    private final QpsTestApplicationService qpsTestApplicationService;

    @Operation(summary = "基础 QPS 测试", description = "不访问 DB 的基础压测接口")
    @GetMapping("/ping")
    public Result<Map<String, Object>> ping() {
        return Result.success(qpsTestApplicationService.ping());
    }

    @Operation(summary = "中等复杂度 QPS 测试", description = "执行 DB 读查询并返回模拟商品数据")
    @GetMapping("/medium")
    public Result<Map<String, Object>> medium() {
        return Result.success(qpsTestApplicationService.medium());
    }

    @Operation(summary = "重度 QPS 测试", description = "执行 DB 读查询、CPU 计算并返回约 50KB 数据")
    @GetMapping("/heavy")
    public Result<Map<String, Object>> heavy() {
        return Result.success(qpsTestApplicationService.heavy());
    }
}
