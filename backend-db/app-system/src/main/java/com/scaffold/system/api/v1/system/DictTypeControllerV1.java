package com.scaffold.system.api.v1.system;

import java.util.List;

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
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;
import com.scaffold.system.domain.dto.system.DictTypeCreateRequestDTO;
import com.scaffold.system.domain.dto.system.DictTypeUpdateRequestDTO;
import com.scaffold.system.domain.query.system.DictTypeQuery;
import com.scaffold.system.domain.service.system.DictApplicationService;
import com.scaffold.system.domain.vo.system.DictTypeVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * DictTypeControllerV1 控制器类
 * <p>
 * 负责提供字典类型的新增、更新、删除、详情和分页查询接口。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-10
 */
@RestController
@RequestMapping("/api/v1/system/dict-types")
@RequiredArgsConstructor
@Tag(name = "DictTypeController", description = "字典类型管理接口")
public class DictTypeControllerV1 {

    private final DictApplicationService dictApplicationService;

    /**
     * 新增字典类型
     */
    @AuditLog(operation = "新增字典类型")
    @Operation(summary = "新增字典类型")
    @PostMapping
    public Result<Boolean> addDictType(@Valid @RequestBody DictTypeCreateRequestDTO request) {
        dictApplicationService.addDictType(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 更新字典类型
     */
    @AuditLog(operation = "更新字典类型")
    @Operation(summary = "更新字典类型")
    @PutMapping("/{id}")
    public Result<Boolean> updateDictType(@PathVariable Long id,
            @Valid @RequestBody DictTypeUpdateRequestDTO request) {
        dictApplicationService.updateDictType(id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 删除字典类型
     */
    @AuditLog(operation = "删除字典类型")
    @Operation(summary = "删除字典类型")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteDictType(@PathVariable Long id) {
        dictApplicationService.deleteDictType(id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 根据ID查询字典类型
     */
    @AuditLog(operation = "查询字典类型", writeDb = false)
    @Operation(summary = "根据ID查询字典类型")
    @GetMapping("/{id}")
    public Result<DictTypeVO> getDictType(@PathVariable Long id) {
        return Result.success(dictApplicationService.getDictType(id));
    }

    /**
     * 分页查询字典类型
     */
    @AuditLog(operation = "分页查询字典类型", writeDb = false)
    @Operation(summary = "分页查询字典类型")
    @GetMapping
    public Result<PageDTO<DictTypeVO>> queryDictTypes(@RequestParam int page, @RequestParam int pageSize,
            DictTypeQuery query) {
        return Result.success(dictApplicationService.queryDictTypes(page, pageSize, query));
    }

    /**
     * 查询所有字典类型（用于下拉选择）
     */
    @AuditLog(operation = "查询所有字典类型", writeDb = false)
    @Operation(summary = "查询所有字典类型")
    @GetMapping("/all")
    public Result<List<DictTypeVO>> listAllDictTypes() {
        return Result.success(dictApplicationService.listAllDictTypes());
    }
}
