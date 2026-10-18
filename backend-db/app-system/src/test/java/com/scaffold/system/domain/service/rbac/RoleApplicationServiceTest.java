package com.scaffold.system.domain.service.rbac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import com.scaffold.system.domain.converter.rbac.PermissionConverter;
import com.scaffold.system.domain.converter.rbac.RoleConverter;
import com.scaffold.system.domain.dto.rbac.RoleCreateRequestDTO;
import com.scaffold.system.domain.dto.rbac.RoleUpdateRequestDTO;
import com.scaffold.system.domain.entity.rbac.Role;
import com.scaffold.system.domain.entity.rbac.RolePermission;
import com.scaffold.system.domain.entity.rbac.UserRole;
import com.scaffold.system.domain.repository.rbac.PermissionRepository;
import com.scaffold.system.domain.repository.rbac.RolePermissionRepository;
import com.scaffold.system.domain.repository.rbac.RoleRepository;
import com.scaffold.system.domain.repository.rbac.UserRepository;
import com.scaffold.system.domain.repository.rbac.UserRoleRepository;

class RoleApplicationServiceTest {

    private RoleRepository roleRepository;
    private RolePermissionRepository rolePermissionRepository;
    private UserRoleRepository userRoleRepository;
    private RoleConverter roleConverter;
    private RoleApplicationService roleApplicationService;
    private SseService sseService;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        roleRepository = mock(RoleRepository.class);
        rolePermissionRepository = mock(RolePermissionRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        roleConverter = mock(RoleConverter.class);
        PermissionRepository permissionRepository = mock(PermissionRepository.class);
        PermissionConverter permissionConverter = mock(PermissionConverter.class);
        sseService = mock(SseService.class);
        userRepository = mock(UserRepository.class);
        roleApplicationService = new RoleApplicationService(
                roleRepository,
                rolePermissionRepository,
                userRoleRepository,
                roleConverter,
                permissionRepository,
                permissionConverter,
                sseService,
                userRepository,
                new TaskExecutorAdapter(Runnable::run));

        doAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            if (role.getId() == null) {
                role.setId(200L);
            }
            return null;
        }).when(roleRepository).save(any(Role.class));

        when(userRoleRepository.findByRoleId(200L)).thenReturn(List.of());
    }

    @Test
    void addRoleCreatesRolePermissionAssociations() {
        RoleCreateRequestDTO request = new RoleCreateRequestDTO();
        request.setRoleCode("admin");
        request.setPermissionIds(List.of(10L, 11L));
        Role role = new Role();
        role.setRoleCode("admin");

        when(roleRepository.findByRoleCode("admin")).thenReturn(Optional.empty());
        when(roleConverter.toEntity(request)).thenReturn(role);

        roleApplicationService.addRole(request);

        ArgumentCaptor<List<RolePermission>> permissionsCaptor = captor(List.class);
        verify(rolePermissionRepository).saveBatch(permissionsCaptor.capture());
        assertThat(permissionsCaptor.getValue())
                .extracting(RolePermission::getRoleId, RolePermission::getPermissionId)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(200L, 10L),
                        org.assertj.core.groups.Tuple.tuple(200L, 11L));
    }

    @Test
    void updateRoleSkipsPermissionWritesWhenPermissionsUnchanged() {
        RoleUpdateRequestDTO request = new RoleUpdateRequestDTO();
        request.setRoleName("admin-updated");
        request.setPermissionIds(List.of(12L, 13L));
        Role role = new Role();
        role.setId(200L);
        role.setRoleCode("admin");

        when(roleRepository.findById(200L)).thenReturn(Optional.of(role));
        when(rolePermissionRepository.findByRoleId(200L)).thenReturn(List.of(
                rolePermission(1L, 200L, 12L),
                rolePermission(2L, 200L, 13L)));

        roleApplicationService.updateRole(200L, request);

        verify(rolePermissionRepository, never()).deleteByIds(any());
        verify(rolePermissionRepository, never()).saveBatch(any());
        verifyNoInteractions(sseService);
    }

    @Test
    void updateRoleAppliesPermissionDiffAndNotifiesAffectedUsers() {
        RoleUpdateRequestDTO request = new RoleUpdateRequestDTO();
        request.setRoleName("admin-updated");
        request.setPermissionIds(List.of(13L, 14L));
        Role role = new Role();
        role.setId(200L);
        role.setRoleCode("admin");
        UserRole userRole = UserRole.of(300L, 200L);
        com.scaffold.system.domain.entity.rbac.User user = new com.scaffold.system.domain.entity.rbac.User();
        user.setId(300L);
        user.setUserName("admin");

        when(roleRepository.findById(200L)).thenReturn(Optional.of(role));
        when(rolePermissionRepository.findByRoleId(200L)).thenReturn(List.of(
                rolePermission(1L, 200L, 12L),
                rolePermission(2L, 200L, 13L)));
        when(userRoleRepository.findByRoleId(200L)).thenReturn(List.of(userRole));
        when(userRepository.findByIds(Set.of(300L))).thenReturn(List.of(user));

        roleApplicationService.updateRole(200L, request);

        verify(rolePermissionRepository).deleteByIds(List.of(1L));
        ArgumentCaptor<List<RolePermission>> permissionsCaptor = captor(List.class);
        verify(rolePermissionRepository).saveBatch(permissionsCaptor.capture());
        assertThat(permissionsCaptor.getValue())
                .extracting(RolePermission::getRoleId, RolePermission::getPermissionId)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(200L, 14L));
        verify(userRepository).findByIds(Set.of(300L));
        verify(sseService).broadcastPermissionChange("admin", "角色[admin-updated]的权限已更新");
    }

    @Test
    void updateRoleNormalizesPermissionIds() {
        RoleUpdateRequestDTO request = new RoleUpdateRequestDTO();
        request.setRoleName("admin-updated");
        request.setPermissionIds(Arrays.asList(12L, null, 12L, 13L));
        Role role = new Role();
        role.setId(200L);
        role.setRoleCode("admin");

        when(roleRepository.findById(200L)).thenReturn(Optional.of(role));
        when(rolePermissionRepository.findByRoleId(200L)).thenReturn(List.of());

        roleApplicationService.updateRole(200L, request);

        ArgumentCaptor<List<RolePermission>> permissionsCaptor = captor(List.class);
        verify(rolePermissionRepository).saveBatch(permissionsCaptor.capture());
        assertThat(permissionsCaptor.getValue())
                .extracting(RolePermission::getRoleId, RolePermission::getPermissionId)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(200L, 12L),
                        org.assertj.core.groups.Tuple.tuple(200L, 13L));
    }

    @Test
    void deleteRoleRejectsWhenUserRolesReferenceRole() {
        Role role = new Role();
        role.setId(200L);
        role.setRoleCode("admin");
        when(roleRepository.findById(200L)).thenReturn(Optional.of(role));
        when(userRoleRepository.findByRoleId(200L)).thenReturn(List.of(new UserRole(), new UserRole()));

        assertThatThrownBy(() -> roleApplicationService.deleteRole(200L))
                .isInstanceOf(com.scaffold.share.exception.BusinessException.class)
                .hasMessageContaining("角色已被以下数据使用，无法删除：用户角色绑定 2 条");

        verify(rolePermissionRepository, never()).deleteByRoleId(200L);
        verify(roleRepository, never()).deleteById(200L);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <T> ArgumentCaptor<T> captor(Class<?> type) {
        return (ArgumentCaptor<T>) ArgumentCaptor.forClass((Class) type);
    }

    private static RolePermission rolePermission(Long id, Long roleId, Long permissionId) {
        RolePermission rolePermission = RolePermission.of(roleId, permissionId);
        rolePermission.setId(id);
        return rolePermission;
    }
}
