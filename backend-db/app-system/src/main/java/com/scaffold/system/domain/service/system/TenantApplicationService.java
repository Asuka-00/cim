package com.scaffold.system.domain.service.system;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.enums.StateEnum;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.system.event.TenantCreatedEvent;
import com.scaffold.share.utils.DeleteReferenceUtils;
import com.scaffold.share.utils.PageQueryUtils;
import com.scaffold.system.domain.converter.system.TenantConverter;
import com.scaffold.system.domain.dto.system.TenantCreateRequestDTO;
import com.scaffold.system.domain.dto.system.TenantUpdateRequestDTO;
import com.scaffold.system.domain.entity.system.Tenant;
import com.scaffold.system.domain.query.system.TenantQuery;
import com.scaffold.system.domain.repository.rbac.PermissionRepository;
import com.scaffold.system.domain.repository.rbac.RoleRepository;
import com.scaffold.system.domain.repository.rbac.UserTenantRepository;
import com.scaffold.system.domain.repository.system.SystemConfigRepository;
import com.scaffold.system.domain.repository.system.TenantRepository;
import com.scaffold.system.domain.repository.rbac.UserRepository;
import com.scaffold.system.domain.vo.system.TenantSelectVO;
import com.scaffold.system.domain.vo.system.TenantVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TenantApplicationService服务类
 * <p>
 * 负责处理TenantApplicationService相关业务编排与领域逻辑
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantApplicationService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final UserTenantRepository userTenantRepository;
    private final TenantConverter tenantConverter;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 新增租户
     */
    @Transactional
    public void insertTenant(TenantCreateRequestDTO createRequest) {
        // 检查租户编码是否已存在
        tenantRepository.findByTenantId(createRequest.getTenantId())
                .ifPresent(tenant -> {
                    throw new BusinessException(ErrorCodeConstants.TENANT_CODE_EXISTS,
                            "租户ID已存在: " + createRequest.getTenantId());
                });

        // 创建租户实体
        Tenant tenant = tenantConverter.toEntity(createRequest);

        tenantRepository.save(tenant);
        eventPublisher.publishEvent(new TenantCreatedEvent(tenant.getTenantId()));
        log.info("Tenant created: tenantId={}, id={}", tenant.getTenantId(), tenant.getId());
    }

    /**
     * 更新租户
     */
    @Transactional
    public void updateTenant(Long id, TenantUpdateRequestDTO updateRequest) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.TENANT_NOT_FOUND, "租户不存在: " + id));

        tenantConverter.updateEntity(updateRequest, tenant);

        tenantRepository.update(tenant);
        log.info("Tenant updated: tenantId={}, id={}", tenant.getTenantId(), tenant.getId());
    }

    /**
     * 删除租户
     */
    @Transactional
    public void deleteTenant(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.TENANT_NOT_FOUND, "租户不存在: " + id));

        String tenantId = tenant.getTenantId();
        DeleteReferenceUtils.requireNoReferences(ErrorCodeConstants.TENANT_HAS_BEEN_BOUND, "租户",
                DeleteReferenceUtils.reference("用户成员", userTenantRepository.countByTenantId(String.valueOf(id))),
                DeleteReferenceUtils.reference("用户", userRepository.findAllByTenantId(tenantId).size()),
                DeleteReferenceUtils.reference("角色", roleRepository.findAllByTenantId(tenantId).size()),
                DeleteReferenceUtils.reference("权限", permissionRepository.findAllByTenantId(tenantId).size()),
                DeleteReferenceUtils.reference("系统配置", systemConfigRepository.findAllByTenantId(tenantId).size()));

        tenantRepository.deleteById(id);
        log.info("Tenant deleted: tenantId={}, id={}", tenant.getTenantId(), tenant.getId());
    }

    /**
     * 根据ID查询租户
     */
    public TenantVO getTenantById(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.TENANT_NOT_FOUND, "租户不存在: " + id));

        return tenantConverter.toVO(tenant);
    }

    /**
     * 根据租户编码查询租户
     */
    public TenantVO getTenantByTenantId(String tenantId) {
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCodeConstants.TENANT_NOT_FOUND, "租户不存在: " + tenantId));

        return tenantConverter.toVO(tenant);
    }

    /**
     * 分页查询租户
     */
    public PageDTO<TenantVO> queryTenantList(int page, int pageSize, TenantQuery query) {
        PageDTO<Tenant> tenantPage = tenantRepository.queryTenantPage(page, pageSize, query);

        return PageQueryUtils.map(tenantPage, tenantConverter::toVO);
    }

    /**
     * 下拉列表查询（不限页，返回启用状态的租户）
     */
    public List<TenantSelectVO> getTenantDropdown() {
        List<Tenant> tenants = tenantRepository.findAll();
        List<Tenant> enabledTenants = tenants.stream()
                .filter(t -> StateEnum.ENABLED.equals(t.getEnabled()))
                .collect(Collectors.toList());
        return tenantConverter.toSelectVOList(enabledTenants);
    }
}
