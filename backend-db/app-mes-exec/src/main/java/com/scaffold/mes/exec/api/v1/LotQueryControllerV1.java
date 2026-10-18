package com.scaffold.mes.exec.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotHistory;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.service.LotQueryApplicationService;
import com.scaffold.share.vo.Result;

import lombok.RequiredArgsConstructor;

/**
 * LotQueryControllerV1 Lot 查询
 * <p>
 * 分页、单批、历史和所属 Wafer。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots")
@RequiredArgsConstructor
public class LotQueryControllerV1 {

    private final LotQueryApplicationService service;

    /**
     * 分页查询。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param lotNo Lot 号模糊条件，可空
     * @param status 状态，可空
     * @return 分页结果
     */
    @GetMapping
    public Result<PageDTO<Lot>> page(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String lotNo,
            @RequestParam(required = false) String status) {
        return Result.success(service.page(page, pageSize, lotNo, status));
    }

    /**
     * 查询一批。
     *
     * @param id Lot
     * @return Lot
     */
    @GetMapping("/{id}")
    public Result<Lot> get(@PathVariable Long id) {
        return Result.success(service.get(id));
    }

    /**
     * 查询历史。
     *
     * @param id Lot
     * @return 历史，新的在前
     */
    @GetMapping("/{id}/history")
    public Result<List<LotHistory>> history(@PathVariable Long id) {
        return Result.success(service.history(id));
    }

    /**
     * 查询所属 Wafer。
     *
     * @param id Lot
     * @return Wafer
     */
    @GetMapping("/{id}/wafers")
    public Result<List<Wafer>> wafers(@PathVariable Long id) {
        return Result.success(service.wafers(id));
    }
}
