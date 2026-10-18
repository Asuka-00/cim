package com.scaffold.mes.base.api.v1;

import java.math.BigDecimal;

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
import com.scaffold.mes.base.domain.dto.ProductUnitConvCheck;
import com.scaffold.mes.base.domain.dto.ProductUnitConvSaveRequest;
import com.scaffold.mes.base.domain.entity.ProductUnitConv;
import com.scaffold.mes.base.domain.service.ProductUnitConvApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * ProductUnitConvControllerV1 产品计数换算接口
 * <p>
 * 挂在产品下，提供换算的查询、核对、新增、修改和删除。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/products/{productId}/unit-conversions")
@RequiredArgsConstructor
public class ProductUnitConvControllerV1 {

    private final ProductUnitConvApplicationService service;

    /**
     * 分页查询某个产品的换算。
     *
     * @param productId 产品
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @return 分页结果
     */
    @GetMapping
    public Result<PageDTO<ProductUnitConv>> page(@PathVariable Long productId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(service.page(productId, page, pageSize));
    }

    /**
     * 按系数核对实际数量。
     *
     * @param productId 产品
     * @param fromUnitId 源计数单位
     * @param toUnitId 目标计数单位
     * @param fromQty 源数量
     * @param actualQty 实际目标数量
     * @return 参考数量和偏差
     */
    @GetMapping("/check")
    public Result<ProductUnitConvCheck> check(@PathVariable Long productId, @RequestParam Long fromUnitId,
            @RequestParam Long toUnitId, @RequestParam BigDecimal fromQty, @RequestParam BigDecimal actualQty) {
        return Result.success(service.check(productId, fromUnitId, toUnitId, fromQty, actualQty));
    }

    /**
     * 新增换算。
     *
     * @param productId 产品
     * @param request 换算内容
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新增产品计数换算")
    public Result<Boolean> create(@PathVariable Long productId, @Valid @RequestBody ProductUnitConvSaveRequest request) {
        service.create(productId, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 修改换算。
     *
     * @param productId 产品
     * @param id 主键
     * @param request 换算内容
     * @return 固定为 true
     */
    @PutMapping("/{id}")
    @AuditLog(operation = "修改产品计数换算")
    public Result<Boolean> update(@PathVariable Long productId, @PathVariable Long id,
            @Valid @RequestBody ProductUnitConvSaveRequest request) {
        service.update(productId, id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 删除换算。
     *
     * @param productId 产品
     * @param id 主键
     * @return 固定为 true
     */
    @DeleteMapping("/{id}")
    @AuditLog(operation = "删除产品计数换算")
    public Result<Boolean> delete(@PathVariable Long productId, @PathVariable Long id) {
        service.delete(productId, id);
        return Result.success(Boolean.TRUE);
    }
}
