package com.scaffold.system.api.v1.rbac;

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
import com.scaffold.system.domain.dto.rbac.PermissionCreateRequestDTO;
import com.scaffold.system.domain.dto.rbac.PermissionUpdateRequestDTO;
import com.scaffold.system.domain.query.rbac.PermissionQuery;
import com.scaffold.system.domain.service.rbac.PermissionApplicationService;
import com.scaffold.system.domain.vo.rbac.PermissionVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * PermissionControllerV1控制器类
 * <p>
 * 负责提供PermissionControllerV1相关接口与请求处理能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@RequestMapping("/api/v1/auth/permissions")
@RestController
@RequiredArgsConstructor
@Tag(name = "PermissionController", description = "权限管理功能对外接口")
public class PermissionControllerV1 {

    private final PermissionApplicationService permissionApplicationService;

    /**
     * 添加权限
     *
     * @param createRequest 创建权限请求信息
     * @return 返回值
     */
    @AuditLog(operation = "新增权限")
    @Operation(summary = "新增权限", description = "添加权限")
    @PostMapping
    public Result<Boolean> insertPermission(@Valid @RequestBody PermissionCreateRequestDTO createRequest) {
        permissionApplicationService.addPermission(createRequest);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 删除权限
     *
     * @param id 权限ID
     * @return 返回值
     */
    @AuditLog(operation = "删除权限")
    @Operation(summary = "删除权限", description = "根据id删除权限")
    @DeleteMapping("/{id}")
    public Result<Boolean> deletePermission(@PathVariable("id") Long id) {
        permissionApplicationService.deletePermission(id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 更新权限信息
     *
     * @param id            权限ID（从路径获取）
     * @param updateRequest 更新权限请求信息
     * @return 操作结果
     */
    @AuditLog(operation = "修改权限信息")
    @Operation(summary = "修改权限信息", description = "根据 ID 修改权限信息")
    @PutMapping("/{id}")
    public Result<Boolean> updatePermission(@PathVariable("id") Long id,
            @Valid @RequestBody PermissionUpdateRequestDTO updateRequest) {
        permissionApplicationService.updatePermission(id, updateRequest);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 查询权限信息列表（分页）
     *
     * @param page     当前页码
     * @param pageSize 每页数量
     * @param query    查询条件
     * @return 分页的权限信息列表
     */
    @AuditLog(operation = "查询权限信息", writeDb = false)
    @Operation(summary = "查询权限信息", description = "查询所有权限信息（分页）")
    @GetMapping
    public Result<PageDTO<PermissionVO>> queryPermissionList(
            @RequestParam(name = "page") int page,
            @RequestParam(name = "pageSize") int pageSize,
            PermissionQuery query) {
        return Result.success(permissionApplicationService.queryPermissionList(page, pageSize, query));
    }

    /**
     * 获取权限下拉列表
     *
     * @param query 查询条件
     * @return 权限下拉列表
     */
    @AuditLog(operation = "获取权限下拉列表", writeDb = false)
    @Operation(summary = "获取权限下拉列表", description = "获取权限下拉列表信息")
    @GetMapping("/options")
    public Result<List<PermissionVO>> getDropPermissionList(PermissionQuery query) {
        return Result.success(permissionApplicationService.getDropPermissionList(query));
    }
}
