package com.scaffold.system.domain.service.rbac;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.share.context.TenantContextHolder;
import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.logging.MdcUtils;
import com.scaffold.share.utils.DeleteReferenceUtils;
import com.scaffold.share.utils.PageQueryUtils;
import com.scaffold.system.domain.converter.rbac.PermissionConverter;
import com.scaffold.system.domain.converter.rbac.RoleConverter;
import com.scaffold.system.domain.dto.rbac.RoleCreateRequestDTO;
import com.scaffold.system.domain.dto.rbac.RoleUpdateRequestDTO;
import com.scaffold.system.domain.entity.rbac.Permission;
import com.scaffold.system.domain.entity.rbac.Role;
import com.scaffold.system.domain.entity.rbac.RolePermission;
import com.scaffold.system.domain.query.rbac.RoleQuery;
import com.scaffold.system.domain.repository.rbac.PermissionRepository;
import com.scaffold.system.domain.repository.rbac.RolePermissionRepository;
import com.scaffold.system.domain.repository.rbac.RoleRepository;
import com.scaffold.system.domain.repository.rbac.UserRepository;
import com.scaffold.system.domain.repository.rbac.UserRoleRepository;
import com.scaffold.system.domain.vo.rbac.PermissionVO;
import com.scaffold.system.domain.vo.rbac.RolePermissionVO;
import com.scaffold.system.domain.vo.rbac.RoleVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RoleApplicationService服务类
 * <p>
 * 负责处理RoleApplicationService相关业务编排与领域逻辑
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleApplicationService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleConverter roleConverter;
    private final PermissionRepository permissionRepository;
    private final PermissionConverter permissionConverter;
    private final SseService sseService;
    private final UserRepository userRepository;
    private final AsyncTaskExecutor virtualThreadTaskExecutor;

    /**
     * 添加角色
     */
    @Transactional
    public void addRole(RoleCreateRequestDTO createRequest) {
        if (roleRepository.findByRoleCode(createRequest.getRoleCode()).isPresent()) {
            throw new BusinessException(ErrorCodeConstants.ROLE_CODE_EXISTS,
                    "角色编码已存在: " + createRequest.getRoleCode());
        }

        Role role = roleConverter.toEntity(createRequest);

        roleRepository.save(role);

        List<Long> permissionIds = createRequest.getPermissionIds();
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<RolePermission> rolePermissions = permissionIds.stream()
                    .map(permissionId -> RolePermission.of(role.getId(), permissionId))
                    .collect(Collectors.toList());
            rolePermissionRepository.saveBatch(rolePermissions);

            notifyAffectedUsers(role.getId(), "角色[" + createRequest.getRoleCode() + "]的权限已更新");
        }

        log.info("Role created: roleCode={}, roleId={}", role.getRoleCode(), role.getId());
    }

    /**
     * 删除角色
     */
    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.ROLE_NOT_FOUND, "角色不存在: " + id));

        DeleteReferenceUtils.requireNoReferences(ErrorCodeConstants.ROLE_HAS_BEEN_BOUND, "角色",
                DeleteReferenceUtils.reference("用户角色绑定", userRoleRepository.findByRoleId(id).size()));

        // 先清中间表，再删主实体，避免权限关联残留
        rolePermissionRepository.deleteByRoleId(role.getId());
        roleRepository.deleteById(role.getId());
        log.info("Role deleted: roleCode={}, roleId={}", role.getRoleCode(), role.getId());
    }

    /**
     * 更新角色
     */
    @Transactional
    public void updateRole(Long id, RoleUpdateRequestDTO updateRequest) {
        long startNanos = System.nanoTime();
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.ROLE_NOT_FOUND, "角色不存在: " + id));
        logStage("role.find", id, startNanos);

        long updateStartNanos = System.nanoTime();
        roleConverter.updateEntity(updateRequest, role);
        roleRepository.update(role);
        logStage("role.update", id, updateStartNanos);

        boolean permissionChanged = updateRolePermissions(role.getId(), updateRequest.getPermissionIds());

        if (permissionChanged) {
            notifyAffectedUsersAfterCommit(role.getId(), "角色[" + updateRequest.getRoleName() + "]的权限已更新");
        }
        logStage("role.update-total", id, startNanos);
        log.info("Role updated: roleCode={}, roleId={}", role.getRoleCode(), role.getId());
    }

    private boolean updateRolePermissions(Long roleId, List<Long> permissionIds) {
        long queryPermissionsStartNanos = System.nanoTime();
        List<RolePermission> existingPermissions = rolePermissionRepository.findByRoleId(roleId);
        logStage("role-permission.find-existing", roleId, queryPermissionsStartNanos);

        Set<Long> requestedPermissionIds = normalizeIds(permissionIds);
        Set<Long> existingPermissionIds = existingPermissions.stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toSet());
        Set<Long> permissionIdsToAdd = new LinkedHashSet<>(requestedPermissionIds);
        permissionIdsToAdd.removeAll(existingPermissionIds);
        Set<Long> permissionIdsToRemove = new LinkedHashSet<>(existingPermissionIds);
        permissionIdsToRemove.removeAll(requestedPermissionIds);

        if (!permissionIdsToRemove.isEmpty()) {
            long deletePermissionsStartNanos = System.nanoTime();
            List<Long> rolePermissionIdsToDelete = existingPermissions.stream()
                    .filter(rolePermission -> permissionIdsToRemove.contains(rolePermission.getPermissionId()))
                    .map(RolePermission::getId)
                    .filter(rolePermissionId -> rolePermissionId != null)
                    .toList();
            rolePermissionRepository.deleteByIds(rolePermissionIdsToDelete);
            logStage("role-permission.delete-diff", roleId, deletePermissionsStartNanos);
        }

        if (!permissionIdsToAdd.isEmpty()) {
            long addPermissionsStartNanos = System.nanoTime();
            List<RolePermission> rolePermissions = permissionIdsToAdd.stream()
                    .map(permissionId -> RolePermission.of(roleId, permissionId))
                    .collect(Collectors.toList());
            rolePermissionRepository.saveBatch(rolePermissions);
            logStage("role-permission.save-diff", roleId, addPermissionsStartNanos);
        }

        return !permissionIdsToAdd.isEmpty() || !permissionIdsToRemove.isEmpty();
    }

    /**
     * 分页查询角色
     */
    public PageDTO<RoleVO> queryRoleList(int page, int pageSize, RoleQuery query) {
        PageDTO<Role> rolePage = roleRepository.queryRolePage(page, pageSize, query);

        return PageQueryUtils.map(rolePage, roleConverter::toVO);
    }

    /**
     * 获取角色下拉列表
     */
    public List<RoleVO> getRoleList() {
        return roleConverter.toVOList(roleRepository.findAll());
    }

    /**
     * 获取指定租户的角色下拉列表；tenantId 为空时使用当前会话租户。
     */
    public List<RoleVO> getRoleList(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return getRoleList();
        }
        return TenantContextHolder.runAs(tenantId, () -> getRoleList());
    }

    /**
     * 获取角色权限（区分已有权限与未包含权限）
     */
    public RolePermissionVO getRolePermissions(Long roleId) {
        roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.ROLE_NOT_FOUND, "角色不存在: " + roleId));

        List<Permission> includedList = permissionRepository.findByRoleId(roleId);
        List<PermissionVO> includedPermissions = permissionConverter.toVOList(includedList);

        List<Permission> allList = permissionRepository.findAll();
        Set<Long> includedIds = includedList.stream().map(Permission::getId).collect(Collectors.toSet());
        // 返回“已包含/未包含”两组，前端授权弹窗可以直接渲染双列表而不必再二次分组
        List<PermissionVO> excludedPermissions = allList.stream()
                .filter(p -> !includedIds.contains(p.getId()))
                .collect(Collectors.collectingAndThen(Collectors.toList(), permissionConverter::toVOList));

        RolePermissionVO response = new RolePermissionVO();
        response.setIncludedPermissions(includedPermissions);
        response.setExcludedPermissions(excludedPermissions);
        return response;
    }

    /**
     * 通知拥有指定角色的所有用户权限已变更
     */
    private void notifyAffectedUsers(Long roleId, String message) {
        List<String> userNames = findAffectedUserNames(roleId);
        sendPermissionChangeNotifications(roleId, userNames, message);
    }

    private List<String> findAffectedUserNames(Long roleId) {
        long userQueryStartNanos = System.nanoTime();
        Set<Long> userIds = userRoleRepository.findByRoleId(roleId).stream()
                .map(userRole -> userRole.getUserId())
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            logStage("role.notify-user-query", roleId, userQueryStartNanos);
            return List.of();
        }
        List<String> userNames = userRepository.findByIds(userIds).stream()
                .map(user -> user.getUserName())
                .filter(userName -> userName != null && !userName.isBlank())
                .distinct()
                .toList();
        logStage("role.notify-user-query", roleId, userQueryStartNanos);
        log.info("Role permission change affected users: roleId={}, userCount={}, userNames={}",
                roleId, userNames.size(), userNames);
        return userNames;
    }

    private void sendPermissionChangeNotifications(Long roleId, List<String> userNames, String message) {
        if (userNames.isEmpty()) {
            return;
        }
        long sseStartNanos = System.nanoTime();
        userNames.forEach(userName -> sseService.broadcastPermissionChange(userName, message));
        logStage("role.notify-sse-send", roleId, sseStartNanos);
    }

    private void notifyAffectedUsersAfterCommit(Long roleId, String message) {
        List<String> userNames = findAffectedUserNames(roleId);
        Map<String, String> mdcContext = MdcUtils.captureContext();
        Runnable task = () -> MdcUtils.runWithContext(mdcContext,
                () -> sendPermissionChangeNotifications(roleId, userNames, message));
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            virtualThreadTaskExecutor.execute(task);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                virtualThreadTaskExecutor.execute(task);
            }
        });
    }

    private Set<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return ids.stream()
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void logStage(String stage, Long roleId, long startNanos) {
        log.info("Role update stage: traceId={}, stage={}, roleId={}, durationMs={}",
                MdcUtils.getOrCreateTraceId(), stage, roleId, (System.nanoTime() - startNanos) / 1_000_000);
    }
}
