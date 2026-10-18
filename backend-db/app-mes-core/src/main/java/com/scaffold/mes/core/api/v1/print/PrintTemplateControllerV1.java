package com.scaffold.mes.core.api.v1.print;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.core.domain.dto.print.PrintTemplateCreateRequest;
import com.scaffold.mes.core.domain.dto.print.PrintTemplateResponse;
import com.scaffold.mes.core.domain.entity.print.PrintTemplate;
import com.scaffold.mes.core.domain.service.print.PrintTemplateApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * PrintTemplateControllerV1 打印模板
 * <p>
 * 新建模板，设成默认，按类型查询，并停用模板。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/print-templates")
@RequiredArgsConstructor
public class PrintTemplateControllerV1 {

    private final PrintTemplateApplicationService service;

    /**
     * 按类型列出模板。
     *
     * @param type 模板类型，可空
     * @return 该类型的模板
     */
    @GetMapping
    public Result<List<PrintTemplate>> list(@RequestParam(required = false) String type) {
        return Result.success(service.list(type));
    }

    /**
     * 新建模板。
     *
     * @param request 编码、名称、类型和内容
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新建打印模板")
    public Result<Boolean> create(@Valid @RequestBody PrintTemplateCreateRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 设成该类型的默认模板。
     *
     * @param id 模板
     * @return 固定为 true
     */
    @PostMapping("/{id}/default")
    @AuditLog(operation = "设置默认打印模板")
    public Result<Boolean> setDefault(@PathVariable Long id) {
        service.setDefault(id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 按类型查出默认模板。
     *
     * @param type 模板类型
     * @return 编码、名称和内容
     */
    @GetMapping("/default")
    public Result<PrintTemplateResponse> showDefault(@RequestParam String type) {
        return Result.success(service.showDefault(type));
    }

    /**
     * 停用模板。
     *
     * @param id 模板
     * @return 固定为 true
     */
    @PostMapping("/{id}/disable")
    @AuditLog(operation = "停用打印模板")
    public Result<Boolean> disable(@PathVariable Long id) {
        service.disable(id);
        return Result.success(Boolean.TRUE);
    }
}
