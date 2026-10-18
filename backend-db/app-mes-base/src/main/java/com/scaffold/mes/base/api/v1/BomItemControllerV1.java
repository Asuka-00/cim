package com.scaffold.mes.base.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.base.domain.dto.BomItemSaveRequest;
import com.scaffold.mes.base.domain.entity.BomItem;
import com.scaffold.mes.base.domain.service.BomItemApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * BomItemControllerV1 BOM 行接口
 * <p>
 * 提供某个 BOM 版本下行的查询、新增、修改和删除。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/bom-versions/{bomVersionId}/items")
@RequiredArgsConstructor
public class BomItemControllerV1 {

    private final BomItemApplicationService service;

    /**
     * 列出 BOM 行。
     *
     * @param bomVersionId BOM 版本
     * @return 按行号排序的行
     */
    @GetMapping
    public Result<List<BomItem>> list(@PathVariable Long bomVersionId) {
        return Result.success(service.list(bomVersionId));
    }

    /**
     * 新增 BOM 行。
     *
     * @param bomVersionId BOM 版本
     * @param request 行内容
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新增 BOM 行")
    public Result<Boolean> create(@PathVariable Long bomVersionId, @Valid @RequestBody BomItemSaveRequest request) {
        service.create(bomVersionId, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 修改 BOM 行。
     *
     * @param bomVersionId BOM 版本
     * @param id 行主键
     * @param request 行内容
     * @return 固定为 true
     */
    @PutMapping("/{id}")
    @AuditLog(operation = "修改 BOM 行")
    public Result<Boolean> update(@PathVariable Long bomVersionId, @PathVariable Long id,
            @Valid @RequestBody BomItemSaveRequest request) {
        service.update(bomVersionId, id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 删除 BOM 行。
     *
     * @param bomVersionId BOM 版本
     * @param id 行主键
     * @return 固定为 true
     */
    @DeleteMapping("/{id}")
    @AuditLog(operation = "删除 BOM 行")
    public Result<Boolean> delete(@PathVariable Long bomVersionId, @PathVariable Long id) {
        service.delete(bomVersionId, id);
        return Result.success(Boolean.TRUE);
    }
}
