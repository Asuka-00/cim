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
import com.scaffold.system.domain.dto.rbac.RoleCreateRequestDTO;
import com.scaffold.system.domain.dto.rbac.RoleUpdateRequestDTO;
import com.scaffold.system.domain.query.rbac.RoleQuery;
import com.scaffold.system.domain.service.rbac.RoleApplicationService;
import com.scaffold.system.domain.vo.rbac.RolePermissionVO;
import com.scaffold.system.domain.vo.rbac.RoleVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * RoleControllerV1控制器类
 * <p>
 * 负责提供RoleControllerV1相关接口与请求处理能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@RequestMapping("/api/v1/roles")
@RestController
@RequiredArgsConstructor
@Tag(name = "RoleController", description = "角色基础功能对外接口")
public class RoleControllerV1 {

    private final RoleApplicationService roleApplicationService;

    /**
     * 添加角色
     *
     * @param createRequest 创建角色请求信息（含权限ID列表）
     * @return 返回值
     */
    @AuditLog(operation = "新增角色")
    @Operation(summary = "新增角色", description = "添加角色")
    @PostMapping
    public Result<Boolean> insertRole(@Valid @RequestBody RoleCreateRequestDTO createRequest) {
        roleApplicationService.addRole(createRequest);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 删除角色
     *
     * @param id 角色ID
     * @return 返回值
     */
    @AuditLog(operation = "删除角色")
    @Operation(summary = "删除角色", description = "根据id删除角色")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteRole(@PathVariable("id") Long id) {
        roleApplicationService.deleteRole(id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 更新角色信息
     *
     * @param id            角色ID（从路径获取）
     * @param updateRequest 更新角色请求信息
     * @return 操作结果
     */
    @AuditLog(operation = "修改角色信息")
    @Operation(summary = "修改角色信息", description = "根据 ID 修改角色信息")
    @PutMapping("/{id}")
    public Result<Boolean> updateRole(@PathVariable("id") Long id,
            @Valid @RequestBody RoleUpdateRequestDTO updateRequest) {
        roleApplicationService.updateRole(id, updateRequest);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 查询所有角色信息（分页）
     *
     * @param page     当前页
     * @param pageSize 每页数量
     * @param query    查询条件
     * @return 返回值
     */
    @AuditLog(operation = "查询角色信息", writeDb = false)
    @Operation(summary = "查询角色信息", description = "查询所有角色信息（分页）")
    @GetMapping
    public Result<PageDTO<RoleVO>> queryRoleList(
            @RequestParam(name = "page") int page,
            @RequestParam(name = "pageSize") int pageSize,
            RoleQuery query) {
        return Result.success(roleApplicationService.queryRoleList(page, pageSize, query));
    }

    /**
     * 获取角色下拉列表
     *
     * @return 角色列表
     */
    @AuditLog(operation = "获取角色下拉列表", writeDb = false)
    @Operation(summary = "获取角色下拉列表", description = "获取角色下拉列表信息")
    @GetMapping("/options")
    public Result<List<RoleVO>> getRoleList(
            @RequestParam(value = "tenantId", required = false) String tenantId) {
        return Result.success(roleApplicationService.getRoleList(tenantId));
    }

    /**
     * 获取角色权限（区分已有权限与未包含权限）
     *
     * @param id 角色ID
     * @return 角色权限信息
     */
    @AuditLog(operation = "获取角色权限", writeDb = false)
    @Operation(summary = "获取角色权限", description = "根据角色ID获取已有权限与未包含权限")
    @GetMapping("/{id}/permissions")
    public Result<RolePermissionVO> getRolePermissions(@PathVariable("id") Long id) {
        return Result.success(roleApplicationService.getRolePermissions(id));
    }
}
