package com.scaffold.mes.base.api.v1;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.dto.UomConversionSaveRequest;
import com.scaffold.mes.base.domain.entity.UomConversion;
import com.scaffold.mes.base.domain.service.UomConversionApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * UomConversionControllerV1 物料单位换算接口
 * <p>
 * 提供换算的查询、解析、新增、修改和删除。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/uom-conversions")
@RequiredArgsConstructor
public class UomConversionControllerV1 {

    private final UomConversionApplicationService service;

    /**
     * 分页查询换算。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param fromUomId 源单位，可空
     * @return 分页结果
     */
    @GetMapping
    public Result<PageDTO<UomConversion>> page(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long fromUomId) {
        return Result.success(service.page(page, pageSize, fromUomId));
    }

    /**
     * 按限定物料优先、通用兜底解析换算。
     *
     * @param fromUomId 源单位
     * @param toUomId 目标单位
     * @param materialId 限定物料，可空
     * @return 命中的换算
     */
    @GetMapping("/resolve")
    public Result<UomConversion> resolve(@RequestParam Long fromUomId, @RequestParam Long toUomId,
            @RequestParam(required = false) Long materialId) {
        return Result.success(service.resolve(fromUomId, toUomId, materialId));
    }

    /**
     * 新增换算。
     *
     * @param request 换算内容
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新增物料单位换算")
    public Result<Boolean> create(@Valid @RequestBody UomConversionSaveRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 修改换算。
     *
     * @param id 主键
     * @param request 换算内容
     * @return 固定为 true
     */
    @PutMapping("/{id}")
    @AuditLog(operation = "修改物料单位换算")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody UomConversionSaveRequest request) {
        service.update(id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 删除换算。
     *
     * @param id 主键
     * @return 固定为 true
     */
    @DeleteMapping("/{id}")
    @AuditLog(operation = "删除物料单位换算")
    public Result<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success(Boolean.TRUE);
    }
}
