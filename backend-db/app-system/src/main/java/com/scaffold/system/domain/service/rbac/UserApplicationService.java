package com.scaffold.system.domain.service.rbac;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.context.SessionContextUtils;
import com.scaffold.share.context.TenantContextHolder;
import com.scaffold.share.enums.StateEnum;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.utils.PageQueryUtils;
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
import com.scaffold.system.domain.query.rbac.UserQuery;
import com.scaffold.system.domain.repository.rbac.PermissionRepository;
import com.scaffold.system.domain.repository.rbac.RoleRepository;
import com.scaffold.system.domain.repository.rbac.UserRepository;
import com.scaffold.system.domain.repository.rbac.UserRoleRepository;
import com.scaffold.system.domain.repository.rbac.UserTenantRepository;
import com.scaffold.system.domain.repository.system.TenantRepository;
import com.scaffold.system.domain.vo.rbac.PermissionVO;
import com.scaffold.system.domain.vo.rbac.UserDetailVO;
import com.scaffold.system.domain.vo.rbac.UserTenantMembershipVO;
import com.scaffold.system.domain.vo.rbac.UserVO;
import com.scaffold.system.domain.vo.system.TenantSelectVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * UserApplicationService服务类
 * <p>
 * 负责处理UserApplicationService相关业务编排与领域逻辑
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserApplicationService {

    private static final String DEFAULT_PASSWORD = "123456";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserTenantRepository userTenantRepository;
    private final TenantRepository tenantRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserConverter userConverter;
    private final PermissionConverter permissionConverter;
    private final SseService sseService;
    private final CurrentSessionProvider currentSessionProvider;

    /**
     * 创建用户并关联租户成员资格与角色
     */
    @Transactional
    public void insertUserInfo(UserCreateRequestDTO createRequest) {
        Optional<User> existingUser = userRepository.findByUserName(createRequest.getUserName());
        if (existingUser.isPresent()) {
            throw new BusinessException(ErrorCodeConstants.USER_ALREADY_EXISTS,
                    "用户账号已存在: " + createRequest.getUserName());
        }

        List<UserTenantMembershipDTO> memberships = normalizeMemberships(createRequest.getMemberships());
        User user = userConverter.toEntity(createRequest);
        user.setTenantId(memberships.get(0).getTenantId());
        applyPlatformAdminFlag(user, createRequest.getPlatformAdmin(), true);
        user.initializePassword(passwordEncoder.encode(createRequest.getPassword()));
        userRepository.save(user);

        replaceMembershipsAndRoles(user, List.of(), memberships);
        log.info("User created: userName={}, userId={}", user.getUserName(), user.getId());
    }

    /**
     * 更新用户信息、成员资格及角色
     */
    @Transactional
    public void updateUserInfo(Long id, UserUpdateRequestDTO updateRequest) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.USER_NOT_FOUND, "用户不存在: " + id));

        userConverter.updateEntity(updateRequest, user);
        applyPlatformAdminFlag(user, updateRequest.getPlatformAdmin(), false);
        List<UserTenantMembershipDTO> memberships = normalizeMemberships(updateRequest.getMemberships());
        user.setTenantId(memberships.get(0).getTenantId());
        userRepository.update(user);

        List<UserTenant> oldMemberships = userTenantRepository.findByUserId(user.getId());
        replaceMembershipsAndRoles(user, oldMemberships, memberships);
        log.info("User updated: userName={}, userId={}", user.getUserName(), user.getId());
    }

    /**
     * 删除用户
     */
    @Transactional
    public void deleteUserInfo(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.USER_NOT_FOUND, "用户不存在: " + id));

        List<UserTenant> memberships = userTenantRepository.findByUserId(user.getId());
        for (UserTenant membership : memberships) {
            TenantContextHolder.runAs(membership.getTenantId(),
                    () -> userRoleRepository.deleteByUserId(user.getId()));
        }
        userTenantRepository.deleteByUserId(user.getId());
        userRepository.deleteById(id);
        log.info("User deleted: userName={}, userId={}", user.getUserName(), user.getId());
    }

    /**
     * 根据账号查询用户
     */
    public UserDetailVO getUserByUserName(String userName) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.USER_NOT_FOUND, "用户不存在: " + userName));

        UserDetailVO userVO = userConverter.toDetailView(user);
        fillMemberships(userVO, user.getId());
        return userVO;
    }

    /**
     * 分页查询用户
     */
    public PageDTO<UserVO> queryUserList(int page, int pageSize, UserQuery query) {
        PageDTO<User> userPage = userRepository.queryUserPage(page, pageSize, query);
        Map<String, String> tenantNameById = tenantNameById();
        return PageQueryUtils.map(userPage, user -> {
            UserVO vo = userConverter.toVO(user);
            List<UserTenant> memberships = userTenantRepository.findByUserId(user.getId());
            vo.setTenantNames(memberships.stream()
                    .map(membership -> tenantNameById.getOrDefault(membership.getTenantId(), membership.getTenantId()))
                    .collect(Collectors.joining(", ")));
            return vo;
        });
    }

    /**
     * 当前用户可切换的租户列表
     */
    public List<TenantSelectVO> getCurrentUserTenants() {
        String userName = SessionContextUtils.getCurrentUserName(currentSessionProvider);
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.USER_NOT_FOUND, "用户不存在: " + userName));
        if (PlatformAdminSupport.isPlatformAdmin(user)) {
            return toEnabledTenantSelectList();
        }
        return toTenantSelectList(userTenantRepository.findByUserId(user.getId()));
    }

    /**
     * 重置密码为默认值
     */
    @Transactional
    public void resetPassword(String userName) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.USER_NOT_FOUND, "用户不存在: " + userName));

        user.resetPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        userRepository.update(user);
        log.info("User password reset: userName={}", userName);
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(ChangePasswordRequestDTO request) {
        String userName = SessionContextUtils.getCurrentUserName(currentSessionProvider);
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.USER_NOT_FOUND, "用户不存在: " + userName));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCodeConstants.USER_OLD_PASSWORD_INCORRECT, "旧密码错误");
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.update(user);
        log.info("User password changed: userName={}", userName);
    }

    /**
     * 获取用户权限列表
     */
    public List<PermissionVO> getUserPermissionsByUserName(String userName) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.USER_NOT_FOUND, "用户不存在: " + userName));

        List<Permission> permissions = PlatformAdminSupport.isPlatformAdmin(user)
                ? permissionRepository.findActiveAll()
                : permissionRepository.findByUserId(user.getId());
        return permissionConverter.toVOList(fillMissingParentPermissions(permissions));
    }

    private List<Permission> fillMissingParentPermissions(List<Permission> permissions) {
        Map<Long, Permission> permissionMap = new LinkedHashMap<>();
        for (Permission permission : permissions) {
            if (permission.getId() != null) {
                permissionMap.putIfAbsent(permission.getId(), permission);
            }
        }

        boolean hasMissingParent = permissions.stream().map(Permission::getParentId)
                .anyMatch(parentId -> parentId != null && !permissionMap.containsKey(parentId));
        if (!hasMissingParent) {
            return List.copyOf(permissionMap.values());
        }

        Map<Long, Permission> availableById = permissionRepository.findAll().stream()
                .filter(permission -> permission.getId() != null)
                .collect(Collectors.toMap(Permission::getId, permission -> permission, (left, right) -> left));

        for (Permission permission : permissions) {
            fillParentPermission(permission, permissionMap, availableById);
        }

        return List.copyOf(permissionMap.values());
    }

    private void fillParentPermission(Permission permission, Map<Long, Permission> permissionMap,
            Map<Long, Permission> availableById) {
        Long parentId = permission.getParentId();
        Set<Long> visitedParentIds = new java.util.HashSet<>();
        while (parentId != null && visitedParentIds.add(parentId)) {
            Permission parent = permissionMap.get(parentId);
            if (parent == null) {
                parent = availableById.get(parentId);
                if (parent == null) {
                    log.warn("Permission parent not found while building user menu tree: parentId={}", parentId);
                    return;
                }
                if (parent.getId() != null) {
                    permissionMap.putIfAbsent(parent.getId(), parent);
                }
            }
            parentId = parent.getParentId();
        }
    }

    /**
     * 获取当前用户权限
     */
    public List<PermissionVO> getCurrentUserPermissions() {
        String userName = SessionContextUtils.getCurrentUserName(currentSessionProvider);
        return getUserPermissionsByUserName(userName);
    }

    private List<UserTenantMembershipDTO> normalizeMemberships(List<UserTenantMembershipDTO> memberships) {
        if (memberships == null || memberships.isEmpty()) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "至少选择一个租户");
        }
        Set<String> seen = new LinkedHashSet<>();
        List<UserTenantMembershipDTO> normalized = new ArrayList<>();
        for (UserTenantMembershipDTO membership : memberships) {
            if (membership == null || membership.getTenantId() == null || membership.getTenantId().isBlank()) {
                throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "租户ID不能为空");
            }
            if (!seen.add(membership.getTenantId())) {
                throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "租户不能重复: " + membership.getTenantId());
            }
            requireTenantExists(membership.getTenantId());
            Set<Long> roleIds = normalizeRoleIds(membership.getRoleIds());
            if (roleIds.isEmpty()) {
                throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "每个租户至少选择一个角色");
            }
            UserTenantMembershipDTO copy = new UserTenantMembershipDTO();
            copy.setTenantId(membership.getTenantId());
            copy.setRoleIds(List.copyOf(roleIds));
            normalized.add(copy);
        }
        return normalized;
    }

    private void requireTenantExists(String tenantId) {
        boolean exists = parseTenantPk(tenantId)
                .flatMap(tenantRepository::findById)
                .isPresent()
                || tenantRepository.findByTenantId(tenantId).isPresent();
        if (!exists) {
            throw new BusinessException(ErrorCodeConstants.TENANT_NOT_FOUND, "租户不存在: " + tenantId);
        }
    }

    private void replaceMembershipsAndRoles(User user, List<UserTenant> oldMemberships,
            List<UserTenantMembershipDTO> memberships) {
        Set<String> newTenantIds = memberships.stream()
                .map(UserTenantMembershipDTO::getTenantId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> oldTenantIds = oldMemberships.stream()
                .map(UserTenant::getTenantId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        boolean rolesChanged = false;
        for (String removedTenantId : oldTenantIds) {
            if (!newTenantIds.contains(removedTenantId)) {
                TenantContextHolder.runAs(removedTenantId, () -> userRoleRepository.deleteByUserId(user.getId()));
                rolesChanged = true;
            }
        }
        for (UserTenantMembershipDTO membership : memberships) {
            rolesChanged |= syncRolesForTenant(user.getId(), membership);
        }

        userTenantRepository.deleteByUserId(user.getId());
        List<UserTenant> toSave = new ArrayList<>();
        for (int i = 0; i < memberships.size(); i++) {
            UserTenantMembershipDTO membership = memberships.get(i);
            toSave.add(UserTenant.of(user.getId(), membership.getTenantId(), i == 0));
        }
        userTenantRepository.saveBatch(toSave);

        if (rolesChanged && user.getUserName() != null && !user.getUserName().isBlank()) {
            sseService.broadcastPermissionChange(user.getUserName(), "您的账号权限已更新");
        }
    }

    private boolean syncRolesForTenant(Long userId, UserTenantMembershipDTO membership) {
        return TenantContextHolder.runAs(membership.getTenantId(), () -> {
            Set<Long> requestedRoleIds = normalizeRoleIds(membership.getRoleIds());
            Set<Long> existingRoleIds = userRoleRepository.findByUserId(userId).stream()
                    .map(UserRole::getRoleId)
                    .collect(Collectors.toSet());
            if (existingRoleIds.equals(requestedRoleIds)) {
                return false;
            }
            List<Long> roleIds = List.copyOf(requestedRoleIds);
            List<Role> roles = roleRepository.findAllById(roleIds);
            if (roles.size() != requestedRoleIds.size()) {
                throw new BusinessException(ErrorCodeConstants.ROLE_NOT_FOUND, "部分角色不存在");
            }
            userRoleRepository.deleteByUserId(userId);
            userRoleRepository.saveBatch(roles.stream()
                    .map(role -> UserRole.of(userId, role.getId()))
                    .collect(Collectors.toList()));
            return true;
        });
    }

    private void fillMemberships(UserDetailVO userVO, Long userId) {
        Map<String, String> tenantNameById = tenantNameById();
        List<UserTenant> memberships = userTenantRepository.findByUserId(userId);
        List<UserTenantMembershipVO> views = new ArrayList<>();
        for (UserTenant membership : memberships) {
            UserTenantMembershipVO view = new UserTenantMembershipVO();
            view.setTenantId(membership.getTenantId());
            view.setTenantName(tenantNameById.getOrDefault(membership.getTenantId(), membership.getTenantId()));
            List<String> roleIds = TenantContextHolder.runAs(membership.getTenantId(),
                    () -> userRoleRepository.findByUserId(userId).stream()
                            .map(userRole -> String.valueOf(userRole.getRoleId()))
                            .toList());
            view.setRoleIds(roleIds);
            views.add(view);
        }
        userVO.setMemberships(views);
        userVO.setTenantNames(views.stream()
                .map(UserTenantMembershipVO::getTenantName)
                .collect(Collectors.joining(", ")));
    }

    private void applyPlatformAdminFlag(User target, Boolean requested, boolean creating) {
        if (requested == null) {
            if (creating) {
                target.setPlatformAdmin(false);
            }
            return;
        }
        User actor = currentActor();
        if (!PlatformAdminSupport.isPlatformAdmin(actor)) {
            if (Boolean.TRUE.equals(requested)) {
                throw new BusinessException(ErrorCodeConstants.ACCESS_DENIED, "仅平台超管可设置平台超管");
            }
            if (creating) {
                target.setPlatformAdmin(false);
            }
            return;
        }
        if (PlatformAdminSupport.isPlatformAdmin(target) && !Boolean.TRUE.equals(requested)
                && actor.getId() != null && actor.getId().equals(target.getId())) {
            throw new BusinessException(ErrorCodeConstants.ACCESS_DENIED, "不能取消自己的平台超管");
        }
        target.setPlatformAdmin(Boolean.TRUE.equals(requested));
    }

    private User currentActor() {
        String userName = SessionContextUtils.getCurrentUserName(currentSessionProvider);
        return userRepository.findByUserName(userName)
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.USER_NOT_FOUND, "用户不存在: " + userName));
    }

    private List<TenantSelectVO> toEnabledTenantSelectList() {
        List<TenantSelectVO> result = new ArrayList<>();
        for (Tenant tenant : tenantRepository.findAll()) {
            if (!StateEnum.ENABLED.equals(tenant.getEnabled()) || tenant.getId() == null) {
                continue;
            }
            TenantSelectVO vo = new TenantSelectVO();
            vo.setId(String.valueOf(tenant.getId()));
            vo.setTenantId(tenant.getTenantId());
            vo.setTenantName(tenant.getTenantName());
            result.add(vo);
        }
        return result;
    }

    List<TenantSelectVO> toTenantSelectList(List<UserTenant> memberships) {
        Map<String, Tenant> tenantByPk = tenantByPk();
        List<TenantSelectVO> result = new ArrayList<>();
        for (UserTenant membership : memberships) {
            TenantSelectVO vo = new TenantSelectVO();
            vo.setId(membership.getTenantId());
            Tenant tenant = tenantByPk.get(membership.getTenantId());
            if (tenant != null) {
                vo.setTenantId(tenant.getTenantId());
                vo.setTenantName(tenant.getTenantName());
            }
            result.add(vo);
        }
        return result;
    }

    private Map<String, String> tenantNameById() {
        return tenantByPk().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().getTenantName(),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private Map<String, Tenant> tenantByPk() {
        Map<String, Tenant> map = new LinkedHashMap<>();
        for (Tenant tenant : tenantRepository.findAll()) {
            if (tenant.getId() != null) {
                map.put(String.valueOf(tenant.getId()), tenant);
            }
        }
        return map;
    }

    private Optional<Long> parseTenantPk(String tenantId) {
        try {
            return Optional.of(Long.parseLong(tenantId));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private Set<Long> normalizeRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Set.of();
        }
        return roleIds.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
