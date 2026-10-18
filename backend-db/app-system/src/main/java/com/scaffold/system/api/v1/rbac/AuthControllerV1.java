package com.scaffold.system.api.v1.rbac;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;
import com.scaffold.system.domain.dto.rbac.LoginRequestDTO;
import com.scaffold.system.domain.dto.rbac.RefreshTokenRequestDTO;
import com.scaffold.system.domain.dto.rbac.SwitchTenantRequestDTO;
import com.scaffold.system.domain.service.rbac.AuthApplicationService;
import com.scaffold.system.domain.service.system.BrandSettingsService;
import com.scaffold.system.domain.vo.rbac.LoginVO;
import com.scaffold.system.domain.vo.system.BrandPublicVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * AuthControllerV1控制器类
 * <p>
 * 负责提供AuthControllerV1相关接口与请求处理能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "AuthController", description = "认证接口")
@RequiredArgsConstructor
public class AuthControllerV1 {

    private final AuthApplicationService authApplicationService;
    private final BrandSettingsService brandSettingsService;

    /**
     * 处理登录请求
     */
    @AuditLog(operation = "用户登录")
    @Operation(summary = "登录", description = "用户名密码登录并返回access/refresh token")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequestDTO request) {
        return Result.success(authApplicationService.login(request));
    }

    /**
     * 登录前公开品牌配置（仅 BRAND 分组白名单，按租户）
     */
    @Operation(summary = "公开品牌配置", description = "返回指定租户的品牌文案与 SVG，供登录页/未登录展示；无数据时字段为空由前端回退静态配置")
    @GetMapping("/brand")
    public Result<BrandPublicVO> getPublicBrand(
            @RequestParam(value = "tenantId", required = false) String tenantId) {
        return Result.success(brandSettingsService.loadPublic(tenantId));
    }

    /**
     * 处理登出请求
     */
    @AuditLog(operation = "用户登出")
    @Operation(summary = "登出", description = "使当前access token失效")
    @PostMapping("/logout")
    public Result<Boolean> logout(@RequestHeader("Authorization") String authorization) {
        if (!authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "Authorization格式错误");
        }
        authApplicationService.logout(authorization.substring(7));
        return Result.success(Boolean.TRUE);
    }

    /**
     * 刷新数据
     */
    @AuditLog(operation = "刷新短token")
    @Operation(summary = "刷新短token", description = "使用refresh token刷新access token")
    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
        return Result.success(authApplicationService.refreshAccessToken(request.getRefreshToken()));
    }

    /**
     * 切换当前租户并重签 token
     */
    @AuditLog(operation = "切换租户")
    @Operation(summary = "切换租户", description = "校验成员资格后重签 access/refresh")
    @PostMapping("/switch-tenant")
    public Result<LoginVO> switchTenant(@Valid @RequestBody SwitchTenantRequestDTO request) {
        return Result.success(authApplicationService.switchTenant(request.getTenantId()));
    }
}
