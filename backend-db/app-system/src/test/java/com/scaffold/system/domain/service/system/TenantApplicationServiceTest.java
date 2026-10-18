package com.scaffold.system.domain.service.system;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.scaffold.share.exception.BusinessException;
import com.scaffold.system.domain.converter.system.TenantConverter;
import com.scaffold.system.domain.entity.system.SystemConfig;
import com.scaffold.system.domain.entity.system.Tenant;
import com.scaffold.system.domain.entity.rbac.User;
import com.scaffold.system.domain.repository.rbac.PermissionRepository;
import com.scaffold.system.domain.repository.rbac.RoleRepository;
import com.scaffold.system.domain.repository.system.SystemConfigRepository;
import com.scaffold.system.domain.repository.system.TenantRepository;
import com.scaffold.system.domain.repository.rbac.UserRepository;
import com.scaffold.system.domain.repository.rbac.UserTenantRepository;

class TenantApplicationServiceTest {

    private TenantRepository tenantRepository;
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PermissionRepository permissionRepository;
    private SystemConfigRepository systemConfigRepository;
    private UserTenantRepository userTenantRepository;
    private TenantApplicationService tenantApplicationService;

    @BeforeEach
    void setUp() {
        tenantRepository = mock(TenantRepository.class);
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        permissionRepository = mock(PermissionRepository.class);
        systemConfigRepository = mock(SystemConfigRepository.class);
        userTenantRepository = mock(UserTenantRepository.class);
        TenantConverter tenantConverter = mock(TenantConverter.class);
        tenantApplicationService = new TenantApplicationService(
                tenantRepository,
                userRepository,
                roleRepository,
                permissionRepository,
                systemConfigRepository,
                userTenantRepository,
                tenantConverter,
                mock(ApplicationEventPublisher.class));
    }

    @Test
    void deleteTenantRejectsWhenTenantScopedDataExists() {
        Tenant tenant = new Tenant();
        tenant.setId(100L);
        tenant.setTenantId("T01");
        when(tenantRepository.findById(100L)).thenReturn(Optional.of(tenant));
        when(userTenantRepository.countByTenantId("100")).thenReturn(0L);
        when(userRepository.findAllByTenantId("T01")).thenReturn(List.of(new User(), new User()));
        when(roleRepository.findAllByTenantId("T01")).thenReturn(List.of());
        when(permissionRepository.findAllByTenantId("T01")).thenReturn(List.of());
        when(systemConfigRepository.findAllByTenantId("T01")).thenReturn(List.of(new SystemConfig()));

        assertThatThrownBy(() -> tenantApplicationService.deleteTenant(100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("租户已被以下数据使用，无法删除：用户 2 条，系统配置 1 条");

        verify(tenantRepository, never()).deleteById(100L);
    }
}
