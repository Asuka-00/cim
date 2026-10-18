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
import com.scaffold.system.domain.dto.system.DictItemCreateRequestDTO;
import com.scaffold.system.domain.dto.system.DictItemUpdateRequestDTO;
import com.scaffold.system.domain.query.system.DictItemQuery;
import com.scaffold.system.domain.service.system.DictApplicationService;
import com.scaffold.system.domain.vo.system.DictItemVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * DictItemControllerV1 控制器类
 * <p>
 * 负责提供字典选项的新增、更新、删除、分页查询和按字典类型查询启用选项能力。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-10
 */
@RestController
@RequestMapping("/api/v1/system/dict-items")
@RequiredArgsConstructor
@Tag(name = "DictItemController", description = "字典选项管理接口")
public class DictItemControllerV1 {

    private final DictApplicationService dictApplicationService;

    /**
     * 新增字典选项
     */
    @AuditLog(operation = "新增字典选项")
    @Operation(summary = "新增字典选项")
    @PostMapping
    public Result<Boolean> addDictItem(@Valid @RequestBody DictItemCreateRequestDTO request) {
        dictApplicationService.addDictItem(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 更新字典选项
     */
    @AuditLog(operation = "更新字典选项")
    @Operation(summary = "更新字典选项")
    @PutMapping("/{id}")
    public Result<Boolean> updateDictItem(@PathVariable Long id,
            @Valid @RequestBody DictItemUpdateRequestDTO request) {
        dictApplicationService.updateDictItem(id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 删除字典选项
     */
    @AuditLog(operation = "删除字典选项")
    @Operation(summary = "删除字典选项")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteDictItem(@PathVariable Long id) {
        dictApplicationService.deleteDictItem(id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 根据ID查询字典选项
     */
    @AuditLog(operation = "查询字典选项", writeDb = false)
    @Operation(summary = "根据ID查询字典选项")
    @GetMapping("/{id}")
    public Result<DictItemVO> getDictItem(@PathVariable Long id) {
        return Result.success(dictApplicationService.getDictItem(id));
    }

    /**
     * 分页查询字典选项
     */
    @AuditLog(operation = "分页查询字典选项", writeDb = false)
    @Operation(summary = "分页查询字典选项")
    @GetMapping
    public Result<PageDTO<DictItemVO>> queryDictItems(@RequestParam int page, @RequestParam int pageSize,
            DictItemQuery query) {
        return Result.success(dictApplicationService.queryDictItems(page, pageSize, query));
    }

    /**
     * 按字典类型查询启用状态的选项
     */
    @AuditLog(operation = "查询启用字典选项", writeDb = false)
    @Operation(summary = "查询启用字典选项")
    @GetMapping("/type/{dictType}/enabled")
    public Result<List<DictItemVO>> getEnabledItems(@PathVariable String dictType) {
        return Result.success(dictApplicationService.getEnabledItems(dictType));
    }
}
