package com.scaffold.mes.exec.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.LotChildResponse;
import com.scaffold.mes.exec.domain.service.LotGenealogyApplicationService;
import com.scaffold.share.vo.Result;

import lombok.RequiredArgsConstructor;

/**
 * LotGenealogyControllerV1 正向追溯
 * <p>
 * 按父 Lot 列出拆出去的子批。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots")
@RequiredArgsConstructor
public class LotGenealogyControllerV1 {

    private final LotGenealogyApplicationService service;

    /**
     * 列出子批。
     *
     * @param id 父 Lot
     * @return 子批
     */
    @GetMapping("/{id}/children")
    public Result<List<LotChildResponse>> children(@PathVariable Long id) {
        return Result.success(service.children(id));
    }

    /**
     * 找回父 Lot。
     *
     * @param id 子 Lot
     * @return 父 Lot。不是拆出来的时为空
     */
    @GetMapping("/{id}/parent")
    public Result<LotChildResponse> parent(@PathVariable Long id) {
        return Result.success(service.parent(id).orElse(null));
    }

    /**
     * 列出来合入这个 Lot 的来源批。
     *
     * @param id 目标 Lot
     * @return 来源批
     */
    @GetMapping("/{id}/merge-sources")
    public Result<List<LotChildResponse>> mergeSources(@PathVariable Long id) {
        return Result.success(service.mergeSources(id));
    }
}
