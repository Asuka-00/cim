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
import com.scaffold.mes.base.domain.dto.BomSaveRequest;
import com.scaffold.mes.base.domain.entity.Bom;
import com.scaffold.mes.base.domain.service.BomApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * BomControllerV1 BOM 接口
 * <p>
 * 提供 BOM 主数据的查询、新增、修改和删除。版本不在本接口。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/boms")
@RequiredArgsConstructor
public class BomControllerV1 {

    private final BomApplicationService service;

    /**
     * 分页查询 BOM。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param bomCode 编码模糊条件，可空
     * @return 分页结果
     */
    @GetMapping
    public Result<PageDTO<Bom>> page(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String bomCode) {
        return Result.success(service.page(page, pageSize, bomCode));
    }

    /**
     * 新增 BOM。
     *
     * @param request BOM 内容
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新增 BOM")
    public Result<Boolean> create(@Valid @RequestBody BomSaveRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 修改 BOM。
     *
     * @param id 主键
     * @param request BOM 内容
     * @return 固定为 true
     */
    @PutMapping("/{id}")
    @AuditLog(operation = "修改 BOM")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody BomSaveRequest request) {
        service.update(id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 删除 BOM。
     *
     * @param id 主键
     * @return 固定为 true
     */
    @DeleteMapping("/{id}")
    @AuditLog(operation = "删除 BOM")
    public Result<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success(Boolean.TRUE);
    }
}
