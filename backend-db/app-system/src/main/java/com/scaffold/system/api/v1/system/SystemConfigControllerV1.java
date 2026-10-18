package com.scaffold.system.api.v1.system;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;
import com.scaffold.system.domain.dto.system.SystemConfigCreateRequestDTO;
import com.scaffold.system.domain.dto.system.SystemConfigUpdateRequestDTO;
import com.scaffold.system.domain.query.system.SystemConfigQuery;
import com.scaffold.system.domain.service.system.SystemConfigApplicationService;
import com.scaffold.system.domain.vo.system.SystemConfigVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * SystemConfigControllerV1控制器类
 * <p>
 * 负责提供SystemConfigControllerV1相关接口与请求处理能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@RestController
@RequestMapping("/api/v1/system-configs")
@RequiredArgsConstructor
@Tag(name = "SystemConfigController", description = "系统配置管理接口")
public class SystemConfigControllerV1 {

    private final SystemConfigApplicationService systemConfigApplicationService;

    /**
     * 新增系统配置
     *
     * @param createRequest 创建系统配置请求信息
     * @return 返回值
     */
    @AuditLog(operation = "新增系统配置")
    @Operation(summary = "新增系统配置", description = "添加系统配置")
    @PostMapping
    public Result<Boolean> insertSystemConfig(@Valid @RequestBody SystemConfigCreateRequestDTO createRequest) {
        systemConfigApplicationService.insertSystemConfig(createRequest);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 更新系统配置
     *
     * @param id            系统配置ID（从路径获取）
     * @param updateRequest 更新系统配置请求信息
     * @return 操作结果
     */
    @AuditLog(operation = "修改系统配置")
    @Operation(summary = "更新系统配置", description = "更新系统配置")
    @PutMapping("/{id}")
    public Result<Boolean> updateSystemConfig(@PathVariable("id") Long id,
            @Valid @RequestBody SystemConfigUpdateRequestDTO updateRequest) {
        systemConfigApplicationService.updateSystemConfig(id, updateRequest);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 删除系统配置
     *
     * @param id 系统配置ID
     * @return 返回值
     */
    @AuditLog(operation = "删除系统配置")
    @Operation(summary = "删除系统配置", description = "根据id删除系统配置")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteSystemConfig(@PathVariable("id") Long id) {
        systemConfigApplicationService.deleteSystemConfig(id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 根据ID查询系统配置
     *
     * @param id 系统配置ID
     * @return 返回值
     */
    @AuditLog(operation = "查询系统配置", writeDb = false)
    @Operation(summary = "查询系统配置", description = "根据id查询系统配置")
    @GetMapping("/{id}")
    public Result<SystemConfigVO> getSystemConfigById(@PathVariable("id") Long id) {
        SystemConfigVO systemConfigVO = systemConfigApplicationService.getSystemConfigById(id);
        return Result.success(systemConfigVO);
    }

    /**
     * 根据配置编码查询系统配置
     *
     * @param configCode 配置编码
     * @return 返回值
     */
    @AuditLog(operation = "查询系统配置", writeDb = false)
    @Operation(summary = "查询系统配置", description = "根据配置编码查询系统配置")
    @GetMapping("/code/{configCode}")
    public Result<SystemConfigVO> getSystemConfigByCode(@PathVariable("configCode") String configCode) {
        SystemConfigVO systemConfigVO = systemConfigApplicationService.getSystemConfigByCode(configCode);
        return Result.success(systemConfigVO);
    }

    /**
     * 查询所有系统配置（全量）
     *
     * @param query 查询条件
     * @return 返回值
     */
    @AuditLog(operation = "查询系统配置", writeDb = false)
    @Operation(summary = "查询系统配置", description = "查询所有系统配置")
    @GetMapping
    public Result<List<SystemConfigVO>> querySystemConfigList(SystemConfigQuery query) {
        return Result.success(systemConfigApplicationService.querySystemConfigList(query));
    }
}
