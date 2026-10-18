package com.scaffold.mes.core.api.v1.uda;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.core.domain.dto.uda.UdaDefCreateRequest;
import com.scaffold.mes.core.domain.entity.uda.UdaDef;
import com.scaffold.mes.core.domain.service.uda.UdaDefApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * UdaDefControllerV1 自定义属性定义
 * <p>
 * 新建一条定义。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/uda-defs")
@RequiredArgsConstructor
public class UdaDefControllerV1 {

    private final UdaDefApplicationService service;

    /**
     * 列出某个对象类型的定义。
     *
     * @param objectType 对象类型，可空
     * @return 按属性编码排序
     */
    @GetMapping
    public Result<List<UdaDef>> list(@RequestParam(required = false) String objectType) {
        return Result.success(service.list(objectType));
    }

    /**
     * 新建定义。
     *
     * @param request 对象类型、编码、名称和数据类型
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新建自定义属性")
    public Result<Boolean> create(@Valid @RequestBody UdaDefCreateRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }
}
