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
import com.scaffold.system.domain.dto.rbac.ChangePasswordRequestDTO;
import com.scaffold.system.domain.dto.rbac.UserCreateRequestDTO;
import com.scaffold.system.domain.dto.rbac.UserUpdateRequestDTO;
import com.scaffold.system.domain.query.rbac.UserQuery;
import com.scaffold.system.domain.service.rbac.UserApplicationService;
import com.scaffold.system.domain.vo.rbac.PermissionVO;
import com.scaffold.system.domain.vo.rbac.UserVO;
import com.scaffold.system.domain.vo.rbac.UserDetailVO;
import com.scaffold.system.domain.vo.system.TenantSelectVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * UserControllerV1控制器类
 * <p>
 * 负责提供UserControllerV1相关接口与请求处理能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "UserController", description = "用户基础功能对外接口")
public class UserControllerV1 {

    private final UserApplicationService userApplicationService;

    /**
     * 添加用户
     *
     * @param createRequest 创建用户请求信息（包含角色ID列表）
     * @return 返回值
     */
    @AuditLog(operation = "新增用户")
    @Operation(summary = "新增用户", description = "添加用户")
    @PostMapping
    public Result<Boolean> insertUserInfo(@Valid @RequestBody UserCreateRequestDTO createRequest) {
        userApplicationService.insertUserInfo(createRequest);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 更新用户信息
     *
     * @param id            用户ID（从路径获取）
     * @param updateRequest 更新用户请求信息（包含角色ID列表）
     * @return 操作结果
     */
    @AuditLog(operation = "修改用户信息")
    @Operation(summary = "更新用户信息", description = "更新用户信息")
    @PutMapping("/{id}")
    public Result<Boolean> updateUserInfo(@PathVariable("id") Long id,
            @Valid @RequestBody UserUpdateRequestDTO updateRequest) {
        userApplicationService.updateUserInfo(id, updateRequest);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 返回值
     */
    @AuditLog(operation = "删除用户")
    @Operation(summary = "删除用户", description = "根据id删除用户")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteUserInfo(@PathVariable("id") Long id) {
        userApplicationService.deleteUserInfo(id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 当前用户可切换的租户列表
     */
    @AuditLog(operation = "获取当前用户租户", writeDb = false)
    @Operation(summary = "当前用户租户", description = "返回当前登录用户所属租户，供切换使用")
    @GetMapping("/me/tenants")
    public Result<List<TenantSelectVO>> getCurrentUserTenants() {
        return Result.success(userApplicationService.getCurrentUserTenants());
    }

    /**
     * 根据登录名查询用户信息
     *
     * @param userName 登录名
     * @return 返回值
     */
    @AuditLog(operation = "查询用户信息", writeDb = false)
    @Operation(summary = "查询用户信息", description = "根据userName查询用户信息")
    @GetMapping("/{userName}")
    public Result<UserDetailVO> getUserByUserName(@PathVariable("userName") String userName) {
        UserDetailVO userVO = userApplicationService.getUserByUserName(userName);
        return Result.success(userVO);
    }

    /**
     * 查询所有用户信息（分页）
     *
     * @param page     当前页
     * @param pageSize 每页数量
     * @param query    查询条件
     * @return 返回值
     */
    @AuditLog(operation = "查询用户信息", writeDb = false)
    @Operation(summary = "查询用户信息", description = "查询所有用户信息（分页）")
    @GetMapping
    public Result<PageDTO<UserVO>> queryUserList(
            @RequestParam(name = "page") int page,
            @RequestParam(name = "pageSize") int pageSize,
            UserQuery query) {
        return Result.success(userApplicationService.queryUserList(page, pageSize, query));
    }

    /**
     * 根据用户名重置密码
     *
     * @param userName 登录名
     * @return 返回值
     */
    @AuditLog(operation = "重置密码")
    @Operation(summary = "重置密码", description = "根据用户名重置密码")
    @PostMapping("/{userName}/password/reset")
    public Result<Boolean> resetPassword(@PathVariable("userName") String userName) {
        userApplicationService.resetPassword(userName);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 更新密码
     *
     * @param changePasswordRequest 修改密码请求信息
     * @return 返回值
     */
    @AuditLog(operation = "更新密码")
    @Operation(summary = "更新密码", description = "允许用户通过旧密码修改自己的登录密码")
    @PostMapping("/password/change")
    public Result<Boolean> changePassword(@Valid @RequestBody ChangePasswordRequestDTO changePasswordRequest) {
        userApplicationService.changePassword(changePasswordRequest);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 获取当前用户的所有角色权限
     *
     * @return 权限列表
     */
    @AuditLog(operation = "获取用户权限", writeDb = false)
    @Operation(summary = "获取用户权限", description = "根据token获取当前用户所有角色的权限列表")
    @GetMapping("/me/permissions")
    public Result<List<PermissionVO>> getCurrentUserPermissions() {
        List<PermissionVO> permissions = userApplicationService.getCurrentUserPermissions();
        return Result.success(permissions);
    }

    /**
     * 根据登录名获取该用户的所有角色权限
     *
     * @param userName 登录名
     * @return 权限列表
     */
    @AuditLog(operation = "获取指定用户权限", writeDb = false)
    @Operation(summary = "获取指定用户权限", description = "根据登录名获取该用户所有角色的权限列表")
    @GetMapping("/{userName}/permissions")
    public Result<List<PermissionVO>> getUserPermissions(@PathVariable("userName") String userName) {
        List<PermissionVO> permissions = userApplicationService.getUserPermissionsByUserName(userName);
        return Result.success(permissions);
    }
}
