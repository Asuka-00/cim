import type { CommonStateValue } from '@/service/api/common';
import type { PermissionTypeValue, VisibilityStateValue } from '../permission';

export type { PermissionTypeValue, VisibilityStateValue } from '@/service/api/common';
export { PermissionType, VisibilityState } from '@/service/api/common';

export interface PermissionVO {
  id: string;
  tenantId: string;
  createBy: string;
  createAt: string;
  updateBy: string;
  updateAt: string;
  permissionCode: string;
  permissionName: string;
  permissionDescription: string;
  type: PermissionTypeValue;
  path: string;
  pathName: string;
  meta: string;
  component: string;
  permissionOrder: number;
  visible: VisibilityStateValue;
  parentId: string | null;
  enabled: CommonStateValue;
}

export interface PermissionQuery {
  permissionCode?: string;
  permissionName?: string;
  type?: PermissionTypeValue;
  parentId?: string;
  visible?: VisibilityStateValue;
}

export interface PermissionCreateRequestDTO {
  permissionCode: string;
  permissionName: string;
  permissionDescription: string;
  type: PermissionTypeValue;
  path?: string;
  pathName?: string;
  meta?: string;
  component?: string;
  permissionOrder?: number;
  visible?: VisibilityStateValue;
  parentId?: string | null;
  enabled?: CommonStateValue;
}

export interface PermissionUpdateRequestDTO {
  permissionName: string;
  permissionDescription: string;
  type: PermissionTypeValue;
  path?: string;
  pathName?: string;
  meta?: string;
  component?: string;
  permissionOrder?: number;
  visible?: VisibilityStateValue;
  parentId?: string | null;
  enabled?: CommonStateValue;
}
