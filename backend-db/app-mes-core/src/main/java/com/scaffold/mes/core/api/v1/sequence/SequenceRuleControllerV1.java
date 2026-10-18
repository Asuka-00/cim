package com.scaffold.mes.core.api.v1.sequence;

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
import com.scaffold.mes.core.domain.dto.sequence.SequenceRuleSaveRequest;
import com.scaffold.mes.core.domain.entity.sequence.SequenceRule;
import com.scaffold.mes.core.domain.service.sequence.SequenceRuleApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * SequenceRuleControllerV1 编号规则接口
 * <p>
 * 提供编号规则的增删改查与号码预览。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/sequence-rules")
@RequiredArgsConstructor
@Tag(name = "SequenceRule", description = "编号规则")
public class SequenceRuleControllerV1 {

    private final SequenceRuleApplicationService service;

    /**
     * 分页查询编号规则。
     *
     * @param page 页码,从 1 开始
     * @param pageSize 每页条数
     * @param ruleCode 编码模糊条件,可空
     * @return 编号规则分页
     */
    @GetMapping
    @Operation(summary = "分页查询编号规则")
    public Result<PageDTO<SequenceRule>> page(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String ruleCode) {
        return Result.success(service.page(page, pageSize, ruleCode));
    }

    /**
     * 新增编号规则。
     *
     * @param request 规则内容
     * @return 固定为 true
     * @throws BusinessException 编码已存在
     */
    @PostMapping
    @AuditLog(operation = "新增编号规则")
    public Result<Boolean> create(@Valid @RequestBody SequenceRuleSaveRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 修改编号规则。
     *
     * @param id 规则主键
     * @param request 规则内容
     * @return 固定为 true
     * @throws BusinessException 规则不存在
     */
    @PutMapping("/{id}")
    @AuditLog(operation = "修改编号规则")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody SequenceRuleSaveRequest request) {
        service.update(id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 删除编号规则。
     *
     * @param id 规则主键
     * @return 固定为 true
     * @throws BusinessException 规则不存在
     */
    @DeleteMapping("/{id}")
    @AuditLog(operation = "删除编号规则")
    public Result<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 预览下一个号码,不消耗流水。
     *
     * @param ruleCode 规则编码
     * @return 预览号码
     * @throws BusinessException 规则不存在或已停用
     */
    @GetMapping("/{ruleCode}/preview")
    @Operation(summary = "预览下一个号码,不消耗流水")
    public Result<String> preview(@PathVariable String ruleCode) {
        return Result.success(service.preview(ruleCode));
    }
}
