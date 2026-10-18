import type { CommonStateValue } from '@/service/api/common';
import type { PermissionVO } from '@/service/api/rtd/system-management/permission';

export interface RoleVO {
  id: string;
  tenantId: string;
  createBy: string;
  createAt: string;
  updateBy: string;
  updateAt: string;
  roleCode: string;
  roleName: string;
  enabled: CommonStateValue;
}

export interface RoleQuery {
  roleCode?: string;
  roleName?: string;
  enabled?: CommonStateValue;
}

export interface RoleCreateRequestDTO {
  roleCode: string;
  roleName: string;
  enabled: CommonStateValue;
  permissionIds?: string[];
}

export interface RoleUpdateRequestDTO {
  roleName: string;
  enabled: CommonStateValue;
  /** 权限 ID：省略/undefined 不改；[] 清空；非空按 diff 增删 */
  permissionIds?: string[];
}

export interface RolePermissionVO {
  includedPermissions: PermissionVO[];
  excludedPermissions: PermissionVO[];
}
