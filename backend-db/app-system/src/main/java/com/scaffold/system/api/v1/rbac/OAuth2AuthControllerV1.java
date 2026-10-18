package com.scaffold.system.api.v1.rbac;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;
import com.scaffold.system.domain.dto.rbac.OAuth2TicketExchangeRequestDTO;
import com.scaffold.system.domain.service.rbac.OAuth2AuthApplicationService;
import com.scaffold.system.domain.vo.rbac.LoginVO;
import com.scaffold.system.domain.vo.rbac.OAuth2ProviderVO;
import com.scaffold.system.infra.security.rbac.OAuth2LoginSuccessHandler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * OAuth2 Client 认证接口
 * <p>
 * 提供方列表、授权入口（写入租户后跳转 Spring OAuth2）、ticket 换票。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-07-17
 */
@RestController
@RequestMapping("/api/v1/auth/oauth2")
@Tag(name = "OAuth2AuthController", description = "OAuth2 Client 认证接口")
@RequiredArgsConstructor
public class OAuth2AuthControllerV1 {

    private final OAuth2AuthApplicationService oauth2AuthApplicationService;

    /**
     * 列出已配置的 OAuth2 提供方
     */
    @Operation(summary = "OAuth2 提供方列表", description = "返回已启用且已配置的 registration，供登录页展示")
    @GetMapping("/providers")
    public Result<List<OAuth2ProviderVO>> listProviders() {
        return Result.success(oauth2AuthApplicationService.listProviders());
    }

    /**
     * 发起 OAuth2 授权：保存租户到会话后跳转 Spring 授权入口
     */
    @AuditLog(operation = "OAuth2授权跳转", writeDb = false)
    @Operation(summary = "发起 OAuth2 授权", description = "校验 registration 与租户后跳转 /oauth2/authorization/{registrationId}")
    @GetMapping("/authorize")
    public void authorize(@RequestParam("registrationId") String registrationId,
            @RequestParam("tenantId") String tenantId,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (tenantId == null || tenantId.isBlank()) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "租户不能为空");
        }
        oauth2AuthApplicationService.requireEnabledRegistration(registrationId);
        HttpSession session = request.getSession(true);
        session.setAttribute(OAuth2LoginSuccessHandler.SESSION_TENANT_ID, tenantId);
        response.sendRedirect(request.getContextPath() + "/oauth2/authorization/" + registrationId);
    }

    /**
     * 使用一次性 ticket 换取本系统 LoginVO
     */
    @AuditLog(operation = "OAuth2换票")
    @Operation(summary = "OAuth2 ticket 换票", description = "消费一次性 ticket，返回与密码登录相同的 LoginVO")
    @PostMapping("/exchange")
    public Result<LoginVO> exchange(@Valid @RequestBody OAuth2TicketExchangeRequestDTO request) {
        return Result.success(oauth2AuthApplicationService.exchangeTicket(request.getTicket()));
    }
}
