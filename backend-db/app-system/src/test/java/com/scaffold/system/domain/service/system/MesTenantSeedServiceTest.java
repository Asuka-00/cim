package com.scaffold.system.domain.service.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.scaffold.share.context.TenantContextHolder;
import com.scaffold.system.domain.entity.rbac.Permission;
import com.scaffold.system.domain.entity.rbac.Role;
import com.scaffold.system.domain.entity.system.SystemConfig;
import com.scaffold.system.domain.repository.rbac.PermissionRepository;
import com.scaffold.system.domain.repository.rbac.RolePermissionRepository;
import com.scaffold.system.domain.repository.rbac.RoleRepository;
import com.scaffold.system.domain.repository.system.DictItemRepository;
import com.scaffold.system.domain.repository.system.DictTypeRepository;
import com.scaffold.system.domain.repository.system.SystemConfigRepository;

/**
 * 租户种子只补缺失项：已有参数不覆盖；没有 default 角色时创建 MES_ADMIN 并授权。
 */
class MesTenantSeedServiceTest {

    private DictTypeRepository dictTypeRepository;
    private DictItemRepository dictItemRepository;
    private SystemConfigRepository systemConfigRepository;
    private RoleRepository roleRepository;
    private RolePermissionRepository rolePermissionRepository;
    private PermissionRepository permissionRepository;
    private MesTenantSeedService service;

    @BeforeEach
    void setUp() {
        dictTypeRepository = mock(DictTypeRepository.class);
        dictItemRepository = mock(DictItemRepository.class);
        systemConfigRepository = mock(SystemConfigRepository.class);
        roleRepository = mock(RoleRepository.class);
        rolePermissionRepository = mock(RolePermissionRepository.class);
        permissionRepository = mock(PermissionRepository.class);
        when(dictTypeRepository.findByDictType(any())).thenReturn(Optional.empty());
        when(dictItemRepository.findByDictTypeAndItemCode(any(), any())).thenReturn(Optional.empty());
        when(systemConfigRepository.findByConfigCode(any())).thenReturn(Optional.empty());
        when(rolePermissionRepository.findByRoleIdAndPermissionId(any(), any())).thenReturn(Optional.empty());
        when(permissionRepository.findByPermissionCode(any())).thenAnswer(invocation -> {
            Permission permission = new Permission();
            permission.setId(1L);
            permission.setPermissionCode(invocation.getArgument(0));
            return Optional.of(permission);
        });
        service = new MesTenantSeedService(dictTypeRepository, dictItemRepository, systemConfigRepository, roleRepository,
                rolePermissionRepository, permissionRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createsMesAdminWhenDefaultRoleIsMissing() {
        when(roleRepository.findByRoleCode("default")).thenReturn(Optional.empty());
        when(roleRepository.findByRoleCode("MES_ADMIN")).thenReturn(Optional.empty());
        doAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setId(8L);
            return null;
        }).when(roleRepository).save(any());

        service.seed("tenant-b");

        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(roleCaptor.capture());
        assertThat(roleCaptor.getValue().getRoleCode()).isEqualTo("MES_ADMIN");
        verify(rolePermissionRepository, times(66)).save(any());
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void doesNotOverwriteExistingConfigAndReusesDefaultRole() {
        Role existing = new Role();
        existing.setId(3L);
        existing.setRoleCode("default");
        when(roleRepository.findByRoleCode("default")).thenReturn(Optional.of(existing));
        when(systemConfigRepository.findByConfigCode(eq("mes.eqp.state.auto"))).thenReturn(Optional.of(new SystemConfig()));
        when(dictTypeRepository.findByDictType("MES_SCRAP_REASON")).thenReturn(Optional.of(new com.scaffold.system.domain.entity.system.DictType()));
        when(dictItemRepository.findByDictTypeAndItemCode("MES_SCRAP_REASON", "OTHER")).thenReturn(Optional.of(new com.scaffold.system.domain.entity.system.DictItem()));

        service.seed("tenant-a");

        verify(roleRepository, never()).save(any());
        verify(systemConfigRepository, times(7)).save(any());
        verify(dictTypeRepository, times(5)).save(any());
    }
}
