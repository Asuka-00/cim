package com.scaffold.system.api.v1.system;

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
import com.scaffold.system.domain.dto.system.TenantCreateRequestDTO;
import com.scaffold.system.domain.dto.system.TenantUpdateRequestDTO;
import com.scaffold.system.domain.query.system.TenantQuery;
import com.scaffold.system.domain.service.system.TenantApplicationService;
import com.scaffold.system.domain.vo.system.TenantSelectVO;
import com.scaffold.system.domain.vo.system.TenantVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * TenantControllerV1控制器类
 * <p>
 * 负责提供TenantControllerV1相关接口与请求处理能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "TenantController", description = "租户管理接口")
public class TenantControllerV1 {

    private final TenantApplicationService tenantApplicationService;

    /**
     * 新增租户
     */
    @AuditLog(operation = "新增租户")
    @Operation(summary = "新增租户", description = "添加租户")
    @PostMapping
    public Result<Boolean> insertTenant(@Valid @RequestBody TenantCreateRequestDTO createRequest) {
        tenantApplicationService.insertTenant(createRequest);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 更新租户
     */
    @AuditLog(operation = "修改租户")
    @Operation(summary = "更新租户", description = "更新租户")
    @PutMapping("/{id}")
    public Result<Boolean> updateTenant(@PathVariable("id") Long id,
            @Valid @RequestBody TenantUpdateRequestDTO updateRequest) {
        tenantApplicationService.updateTenant(id, updateRequest);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 删除租户
     */
    @AuditLog(operation = "删除租户")
    @Operation(summary = "删除租户", description = "根据id删除租户")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteTenant(@PathVariable("id") Long id) {
        tenantApplicationService.deleteTenant(id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 根据ID查询租户
     */
    @AuditLog(operation = "查询租户", writeDb = false)
    @Operation(summary = "查询租户", description = "根据id查询租户")
    @GetMapping("/{id}")
    public Result<TenantVO> getTenantById(@PathVariable("id") Long id) {
        return Result.success(tenantApplicationService.getTenantById(id));
    }

    /**
     * 根据租户编码查询租户
     */
    @AuditLog(operation = "查询租户", writeDb = false)
    @Operation(summary = "查询租户", description = "根据租户ID查询租户")
    @GetMapping("/tenant-id/{tenantId}")
    public Result<TenantVO> getTenantByTenantId(@PathVariable("tenantId") String tenantId) {
        return Result.success(tenantApplicationService.getTenantByTenantId(tenantId));
    }

    /**
     * 查询所有租户（分页）
     */
    @AuditLog(operation = "查询租户", writeDb = false)
    @Operation(summary = "查询租户", description = "查询所有租户（分页）")
    @GetMapping
    public Result<PageDTO<TenantVO>> queryTenantList(
            @RequestParam(name = "page") int page,
            @RequestParam(name = "pageSize") int pageSize,
            TenantQuery query) {
        return Result.success(tenantApplicationService.queryTenantList(page, pageSize, query));
    }

    /**
     * 下拉列表查询（无需认证）
     */
    @AuditLog(operation = "下拉列表查询", writeDb = false)
    @Operation(summary = "下拉列表查询", description = "获取启用的租户下拉列表，无需认证")
    @GetMapping("/dropdown")
    public Result<List<TenantSelectVO>> getTenantDropdown() {
        return Result.success(tenantApplicationService.getTenantDropdown());
    }
}
