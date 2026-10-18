package com.scaffold.mes.exec.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.entity.WaferHistory;
import com.scaffold.mes.exec.domain.service.WaferQueryApplicationService;
import com.scaffold.share.vo.Result;

import lombok.RequiredArgsConstructor;

/**
 * WaferQueryControllerV1 Wafer 查询
 * <p>
 * 分页、单片和历史。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/wafers")
@RequiredArgsConstructor
public class WaferQueryControllerV1 {

    private final WaferQueryApplicationService service;

    /**
     * 分页查询。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param waferCode 内部编号模糊条件，可空
     * @param lotId Lot，可空
     * @return 分页结果
     */
    @GetMapping
    public Result<PageDTO<Wafer>> page(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String waferCode,
            @RequestParam(required = false) Long lotId) {
        return Result.success(service.page(page, pageSize, waferCode, lotId));
    }

    /**
     * 查询一片。
     *
     * @param id Wafer
     * @return Wafer
     */
    @GetMapping("/{id}")
    public Result<Wafer> get(@PathVariable Long id) {
        return Result.success(service.get(id));
    }

    /**
     * 查询历史。
     *
     * @param id Wafer
     * @return 历史，新的在前
     */
    @GetMapping("/{id}/history")
    public Result<List<WaferHistory>> history(@PathVariable Long id) {
        return Result.success(service.history(id));
    }
}
