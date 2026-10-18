package com.scaffold.mes.base.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.base.domain.dto.UserScopeCreateRequest;
import com.scaffold.mes.base.domain.entity.UserScope;
import com.scaffold.mes.base.domain.service.UserScopeApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * UserScopeControllerV1 用户数据权限
 * <p>
 * 查询时没有范围记录表示不限制。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/user-scopes")
@RequiredArgsConstructor
public class UserScopeControllerV1 {

    private final UserScopeApplicationService service;

    /**
     * 授予范围。
     *
     * @param request 用户、类型和对象
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "授予数据权限")
    public Result<Boolean> create(@Valid @RequestBody UserScopeCreateRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 判断能否操作。
     *
     * @param userName 用户
     * @param scopeType 类型
     * @param scopeId 对象
     * @return 允许时为 true
     */
    @GetMapping("/allowed")
    public Result<Boolean> allowed(@RequestParam String userName, @RequestParam String scopeType,
            @RequestParam Long scopeId) {
        return Result.success(service.allowed(userName, scopeType, scopeId));
    }

    /**
     * 列出全部范围。
     *
     * @return 按用户名排列
     */
    @GetMapping
    public Result<List<UserScope>> list() {
        return Result.success(service.list());
    }
}
