package com.scaffold.mes.warehouse.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.warehouse.domain.entity.MaterialLot;
import com.scaffold.mes.warehouse.domain.service.MaterialLotQueryApplicationService;
import com.scaffold.share.vo.Result;

import lombok.RequiredArgsConstructor;

/**
 * MaterialLotControllerV1 物料批查询
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/material-lots")
@RequiredArgsConstructor
public class MaterialLotControllerV1 {

    private final MaterialLotQueryApplicationService service;

    /**
     * 列出物料批。
     *
     * @return 按批号排列
     */
    @GetMapping
    public Result<List<MaterialLot>> list() {
        return Result.success(service.list());
    }
}
