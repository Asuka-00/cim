package com.scaffold.system.domain.service.rbac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.enums.StateEnum;
import com.scaffold.system.domain.converter.rbac.PermissionConverter;
import com.scaffold.system.domain.converter.rbac.UserConverter;
import com.scaffold.system.domain.dto.rbac.ChangePasswordRequestDTO;
import com.scaffold.system.domain.dto.rbac.UserCreateRequestDTO;
import com.scaffold.system.domain.dto.rbac.UserTenantMembershipDTO;
import com.scaffold.system.domain.dto.rbac.UserUpdateRequestDTO;
import com.scaffold.system.domain.entity.rbac.Permission;
import com.scaffold.system.domain.entity.rbac.Role;
import com.scaffold.system.domain.entity.rbac.User;
import com.scaffold.system.domain.entity.rbac.UserRole;
import com.scaffold.system.domain.entity.rbac.UserTenant;
import com.scaffold.system.domain.entity.system.Tenant;
import com.scaffold.system.domain.repository.rbac.PermissionRepository;
import com.scaffold.system.domain.repository.rbac.RoleRepository;
import com.scaffold.system.domain.repository.rbac.UserRepository;
import com.scaffold.system.domain.repository.rbac.UserRoleRepository;
import com.scaffold.system.domain.repository.rbac.UserTenantRepository;
import com.scaffold.system.domain.repository.system.TenantRepository;
import com.scaffold.system.domain.vo.rbac.PermissionVO;
import com.scaffold.system.domain.vo.system.TenantSelectVO;

class UserApplicationServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private UserRoleRepository userRoleRepository;
    private UserTenantRepository userTenantRepository;
    private TenantRepository tenantRepository;
    private PermissionRepository permissionRepository;
    private PasswordEncoder passwordEncoder;
    private UserConverter userConverter;
    private PermissionConverter permissionConverter;
    private SseService sseService;
    private CurrentSessionProvider currentSessionProvider;
    private UserApplicationService userApplicationService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        userTenantRepository = mock(UserTenantRepository.class);
        tenantRepository = mock(TenantRepository.class);
        permissionRepository = mock(PermissionRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userConverter = mock(UserConverter.class);
        permissionConverter = mock(PermissionConverter.class);
        sseService = mock(SseService.class);
        currentSessionProvider = mock(CurrentSessionProvider.class);
        userApplicationService = new UserApplicationService(userRepository, roleRepository, userRoleRepository,
                userTenantRepository, tenantRepository, permissionRepository, passwordEncoder, userConverter,
                permissionConverter, sseService, currentSessionProvider);

        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(100L);
            }
            return null;
        }).when(userRepository).save(any(User.class));
    }

    @Test
    void insertUserInfoStoresMembershipsAndRoles() {
        UserCreateRequestDTO request = new UserCreateRequestDTO();
        request.setUserName("u001");
        request.setPassword("raw-password");
        request.setMemberships(List.of(membership("t1", List.of(10L, 11L))));
        User user = new User();
        user.setUserName("u001");

        when(userRepository.findByUserName("u001")).thenReturn(Optional.empty());
        when(userConverter.toEntity(request)).thenReturn(user);
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(tenantRepository.findById(any())).thenReturn(Optional.empty());
        when(tenantRepository.findByTenantId("t1")).thenReturn(Optional.of(tenant(1L, "t1")));
        when(roleRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(role(10L), role(11L)));
        when(userRoleRepository.findByUserId(100L)).thenReturn(List.of());

        userApplicationService.insertUserInfo(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("encoded-password");
        assertThat(userCaptor.getValue().getTenantId()).isEqualTo("t1");

        ArgumentCaptor<List<UserTenant>> membershipCaptor = captor(List.class);
        verify(userTenantRepository).saveBatch(membershipCaptor.capture());
        assertThat(membershipCaptor.getValue()).extracting(UserTenant::getTenantId, UserTenant::getIsDefault)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("t1", true));

        ArgumentCaptor<List<UserRole>> userRolesCaptor = captor(List.class);
        verify(userRoleRepository).saveBatch(userRolesCaptor.capture());
        assertThat(userRolesCaptor.getValue()).extracting(UserRole::getUserId, UserRole::getRoleId).containsExactly(
                org.assertj.core.groups.Tuple.tuple(100L, 10L), org.assertj.core.groups.Tuple.tuple(100L, 11L));
    }

    @Test
    void insertUserInfoStoresMultipleMemberships() {
        UserCreateRequestDTO request = new UserCreateRequestDTO();
        request.setUserName("u001");
        request.setPassword("raw-password");
        request.setMemberships(List.of(
                membership("t1", List.of(10L)),
                membership("t2", List.of(20L))));
        User user = new User();
        user.setUserName("u001");

        when(userRepository.findByUserName("u001")).thenReturn(Optional.empty());
        when(userConverter.toEntity(request)).thenReturn(user);
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(tenantRepository.findById(any())).thenReturn(Optional.empty());
        when(tenantRepository.findByTenantId("t1")).thenReturn(Optional.of(tenant(1L, "t1")));
        when(tenantRepository.findByTenantId("t2")).thenReturn(Optional.of(tenant(2L, "t2")));
        when(roleRepository.findAllById(List.of(10L))).thenReturn(List.of(role(10L)));
        when(roleRepository.findAllById(List.of(20L))).thenReturn(List.of(role(20L)));
        when(userRoleRepository.findByUserId(100L)).thenReturn(List.of());

        userApplicationService.insertUserInfo(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getTenantId()).isEqualTo("t1");

        ArgumentCaptor<List<UserTenant>> membershipCaptor = captor(List.class);
        verify(userTenantRepository).saveBatch(membershipCaptor.capture());
        assertThat(membershipCaptor.getValue()).extracting(UserTenant::getTenantId, UserTenant::getIsDefault)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("t1", true),
                        org.assertj.core.groups.Tuple.tuple("t2", false));

        verify(userRoleRepository, times(2)).saveBatch(any());
    }

    @Test
    void updateUserInfoDoesNotDeleteRolesOfUnrelatedTenant() {
        UserUpdateRequestDTO request = updateRequest(List.of(membership("t1", List.of(10L))));
        User user = user(100L, "u001");
        when(userRepository.findById(100L)).thenReturn(Optional.of(user));
        when(userTenantRepository.findByUserId(100L)).thenReturn(List.of(
                UserTenant.of(100L, "t1", true),
                UserTenant.of(100L, "t2", false)));
        when(tenantRepository.findById(any())).thenReturn(Optional.empty());
        when(tenantRepository.findByTenantId("t1")).thenReturn(Optional.of(tenant(1L, "t1")));
        when(userRoleRepository.findByUserId(100L)).thenReturn(List.of(userRole(100L, 10L)));

        userApplicationService.updateUserInfo(100L, request);

        verify(userRoleRepository, times(1)).deleteByUserId(100L);
        verify(userRoleRepository, never()).saveBatch(any());
        verify(sseService).broadcastPermissionChange("u001", "您的账号权限已更新");
    }

    @Test
    void resetPasswordStoresEncodedDefaultPassword() {
        User user = new User();
        user.setUserName("u001");
        user.setPasswordHash("old-password");

        when(userRepository.findByUserName("u001")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("123456")).thenReturn("encoded-default");

        userApplicationService.resetPassword("u001");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).update(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("encoded-default");
    }

    @Test
    void changePasswordValidatesOldPasswordAndStoresEncodedNewPassword() {
        ChangePasswordRequestDTO request = new ChangePasswordRequestDTO();
        request.setOldPassword("old-raw");
        request.setNewPassword("new-raw");
        User user = new User();
        user.setUserName("u001");
        user.setPasswordHash("old-encoded");

        when(currentSessionProvider.getCurrentUserName()).thenReturn("u001");
        when(userRepository.findByUserName("u001")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-raw", "old-encoded")).thenReturn(true);
        when(passwordEncoder.encode("new-raw")).thenReturn("new-encoded");

        userApplicationService.changePassword(request);

        verify(passwordEncoder).matches("old-raw", "old-encoded");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).update(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("new-encoded");
    }

    @Test
    void getCurrentUserTenantsReturnsAllEnabledForPlatformAdmin() {
        User admin = user(9L, "admin");
        admin.setPlatformAdmin(true);
        Tenant enabled = tenant(1L, "t1");
        enabled.setEnabled(StateEnum.ENABLED);
        Tenant disabled = tenant(2L, "t2");
        disabled.setEnabled(StateEnum.DISABLED);

        when(currentSessionProvider.getCurrentUserName()).thenReturn("admin");
        when(userRepository.findByUserName("admin")).thenReturn(Optional.of(admin));
        when(tenantRepository.findAll()).thenReturn(List.of(enabled, disabled));

        List<TenantSelectVO> result = userApplicationService.getCurrentUserTenants();

        assertThat(result).extracting(TenantSelectVO::getId, TenantSelectVO::getTenantId)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("1", "t1"));
        verify(userTenantRepository, never()).findByUserId(any());
    }

    @Test
    void getUserPermissionsByUserNameReturnsFullCatalogForPlatformAdmin() {
        User admin = user(100L, "admin");
        admin.setPlatformAdmin(true);
        Permission catalog = permission(1L, null);
        List<PermissionVO> expectedVos = List.of(new PermissionVO());

        when(userRepository.findByUserName("admin")).thenReturn(Optional.of(admin));
        when(permissionRepository.findActiveAll()).thenReturn(List.of(catalog));
        when(permissionConverter.toVOList(any())).thenReturn(expectedVos);

        List<PermissionVO> result = userApplicationService.getUserPermissionsByUserName("admin");

        assertThat(result).isSameAs(expectedVos);
        verify(permissionRepository).findActiveAll();
        verify(permissionRepository, never()).findByUserId(any());
    }

    @Test
    void getUserPermissionsByUserNameIncludesMissingParentPermissions() {
        User user = user(100L, "u001");
        Permission child = permission(300L, 200L);
        Permission parent = permission(200L, 100L);
        Permission root = permission(100L, null);
        List<PermissionVO> expectedVos = List.of(new PermissionVO(), new PermissionVO(), new PermissionVO());

        when(userRepository.findByUserName("u001")).thenReturn(Optional.of(user));
        when(permissionRepository.findByUserId(100L)).thenReturn(List.of(child));
        when(permissionRepository.findAll()).thenReturn(List.of(child, parent, root));
        when(permissionConverter.toVOList(any())).thenReturn(expectedVos);

        List<PermissionVO> result = userApplicationService.getUserPermissionsByUserName("u001");

        assertThat(result).isSameAs(expectedVos);
        ArgumentCaptor<List<Permission>> permissionsCaptor = captor(List.class);
        verify(permissionConverter).toVOList(permissionsCaptor.capture());
        assertThat(permissionsCaptor.getValue()).extracting(Permission::getId).containsExactly(300L, 200L, 100L);
        verify(permissionRepository).findAll();
        verify(permissionRepository, never()).findById(any());
    }

    @Test
    void getUserPermissionsByUserNameDeduplicatesExistingParentPermissions() {
        User user = user(100L, "u001");
        Permission child = permission(300L, 200L);
        Permission parent = permission(200L, null);
        List<PermissionVO> expectedVos = List.of(new PermissionVO(), new PermissionVO());

        when(userRepository.findByUserName("u001")).thenReturn(Optional.of(user));
        when(permissionRepository.findByUserId(100L)).thenReturn(List.of(child, parent, child));
        when(permissionConverter.toVOList(any())).thenReturn(expectedVos);

        List<PermissionVO> result = userApplicationService.getUserPermissionsByUserName("u001");

        assertThat(result).isSameAs(expectedVos);
        ArgumentCaptor<List<Permission>> permissionsCaptor = captor(List.class);
        verify(permissionConverter).toVOList(permissionsCaptor.capture());
        assertThat(permissionsCaptor.getValue()).extracting(Permission::getId).containsExactly(300L, 200L);
        verify(permissionRepository, never()).findAll();
        verify(permissionRepository, never()).findById(any());
    }

    @Test
    void getUserPermissionsByUserNameSkipsMissingParentPermissions() {
        User user = user(100L, "u001");
        Permission child = permission(300L, 200L);
        List<PermissionVO> expectedVos = List.of(new PermissionVO());

        when(userRepository.findByUserName("u001")).thenReturn(Optional.of(user));
        when(permissionRepository.findByUserId(100L)).thenReturn(List.of(child));
        when(permissionRepository.findAll()).thenReturn(List.of(child));
        when(permissionConverter.toVOList(any())).thenReturn(expectedVos);

        List<PermissionVO> result = userApplicationService.getUserPermissionsByUserName("u001");

        assertThat(result).isSameAs(expectedVos);
        ArgumentCaptor<List<Permission>> permissionsCaptor = captor(List.class);
        verify(permissionConverter).toVOList(permissionsCaptor.capture());
        assertThat(permissionsCaptor.getValue()).extracting(Permission::getId).containsExactly(300L);
    }

    private Role role(Long id) {
        Role role = new Role();
        role.setId(id);
        role.setEnabled(StateEnum.ENABLED);
        return role;
    }

    private User user(Long id, String userName) {
        User user = new User();
        user.setId(id);
        user.setUserName(userName);
        return user;
    }

    private UserRole userRole(Long userId, Long roleId) {
        return UserRole.of(userId, roleId);
    }

    private Permission permission(Long id, Long parentId) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setParentId(parentId);
        return permission;
    }

    private Tenant tenant(Long id, String tenantId) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setTenantId(tenantId);
        tenant.setTenantName(tenantId);
        return tenant;
    }

    private UserTenantMembershipDTO membership(String tenantId, List<Long> roleIds) {
        UserTenantMembershipDTO dto = new UserTenantMembershipDTO();
        dto.setTenantId(tenantId);
        dto.setRoleIds(roleIds);
        return dto;
    }

    private UserUpdateRequestDTO updateRequest(List<UserTenantMembershipDTO> memberships) {
        UserUpdateRequestDTO request = new UserUpdateRequestDTO();
        request.setDisplayName("User 001");
        request.setEnabled(StateEnum.ENABLED);
        request.setMemberships(memberships);
        return request;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <T> ArgumentCaptor<T> captor(Class<?> type) {
        return (ArgumentCaptor<T>) ArgumentCaptor.forClass((Class) type);
    }
}
