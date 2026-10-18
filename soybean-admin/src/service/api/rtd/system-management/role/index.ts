import type { PageDTO } from '@/service/api/common';
import type { RoleCreateRequestDTO, RolePermissionVO, RoleQuery, RoleUpdateRequestDTO, RoleVO } from './types';
import { request } from '@/service/request';

export type { RoleCreateRequestDTO, RolePermissionVO, RoleQuery, RoleUpdateRequestDTO, RoleVO } from './types';

/** 分页查询角色列表 */
export const getRoleList = (
  params: {
    page: number;
    pageSize: number;
  } & Partial<RoleQuery>
) => {
  return request<PageDTO<RoleVO>>({ url: '/api/v1/roles', method: 'get', params });
};

/** 获取角色下拉列表 */
export const getRoleOptions = (tenantId?: string) => {
  return request<RoleVO[]>({
    url: '/api/v1/roles/options',
    method: 'get',
    params: tenantId ? { tenantId } : undefined
  });
};

/** 新增角色 */
export const createRole = (data: RoleCreateRequestDTO) => {
  return request<boolean>({ url: '/api/v1/roles', method: 'post', data });
};

/** 更新角色 */
export const updateRole = (id: string, data: RoleUpdateRequestDTO) => {
  return request<boolean>({ url: `/api/v1/roles/${id}`, method: 'put', data });
};

/** 删除角色 */
export const deleteRole = (id: string) => {
  return request<boolean>({ url: `/api/v1/roles/${id}`, method: 'delete' });
};

/** 获取角色权限（包含已分配和未分配） */
export const getRolePermissions = (id: string) => {
  return request<RolePermissionVO>({ url: `/api/v1/roles/${id}/permissions`, method: 'get' });
};
