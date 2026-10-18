package com.scaffold.mes.base.api.v1;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.base.domain.dto.UserQualCreateRequest;
import com.scaffold.mes.base.domain.entity.UserQual;
import com.scaffold.mes.base.domain.service.UserQualApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * UserQualControllerV1 人员资质
 * <p>
 * 到期后的资质标成 EXPIRED，不再视为有效。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/user-quals")
@RequiredArgsConstructor
public class UserQualControllerV1 {

    private final UserQualApplicationService service;

    /**
     * 新建资质。
     *
     * @param request 用户和目标
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新建人员资质")
    public Result<Boolean> create(@Valid @RequestBody UserQualCreateRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 把已到期的资质标成过期。
     *
     * @param now 当前时间，空则取系统时间
     * @return 改了几条
     */
    @PostMapping("/expire-due")
    @AuditLog(operation = "人员资质过期")
    public Result<Integer> expire(@RequestParam(required = false) LocalDateTime now) {
        return Result.success(service.expireDue(now == null ? LocalDateTime.now() : now));
    }

    /**
     * 列出全部资质。
     *
     * @return 按用户名排列
     */
    @GetMapping
    public Result<List<UserQual>> list() {
        return Result.success(service.list());
    }
}
