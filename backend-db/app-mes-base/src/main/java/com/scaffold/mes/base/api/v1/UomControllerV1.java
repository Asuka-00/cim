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
import com.scaffold.mes.base.domain.dto.UomSaveRequest;
import com.scaffold.mes.base.domain.entity.Uom;
import com.scaffold.mes.base.domain.service.UomApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * UomControllerV1 物料计量单位接口
 * <p>
 * 提供物料计量单位的查询、新增、修改和删除。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/uoms")
@RequiredArgsConstructor
public class UomControllerV1 {

    private final UomApplicationService service;

    /**
     * 分页查询物料计量单位。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param uomCode 编码模糊条件，可空
     * @return 分页结果
     */
    @GetMapping
    public Result<PageDTO<Uom>> page(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String uomCode) {
        return Result.success(service.page(page, pageSize, uomCode));
    }

    /**
     * 新增物料计量单位。
     *
     * @param request 单位内容
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新增物料计量单位")
    public Result<Boolean> create(@Valid @RequestBody UomSaveRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 修改物料计量单位。
     *
     * @param id 主键
     * @param request 单位内容
     * @return 固定为 true
     */
    @PutMapping("/{id}")
    @AuditLog(operation = "修改物料计量单位")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody UomSaveRequest request) {
        service.update(id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 删除物料计量单位。
     *
     * @param id 主键
     * @return 固定为 true
     */
    @DeleteMapping("/{id}")
    @AuditLog(operation = "删除物料计量单位")
    public Result<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success(Boolean.TRUE);
    }
}
