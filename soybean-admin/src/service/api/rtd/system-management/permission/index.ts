import type { PageDTO } from '@/service/api/common';
import type { PermissionCreateRequestDTO, PermissionQuery, PermissionUpdateRequestDTO, PermissionVO } from './types';
import { request } from '@/service/request';

export type {
  PermissionCreateRequestDTO,
  PermissionQuery,
  PermissionTypeValue,
  PermissionUpdateRequestDTO,
  PermissionVO,
  VisibilityStateValue
} from './types';
export { PermissionType, VisibilityState } from './types';

/** 分页查询权限列表 */
export const getPermissionList = (
  params: {
    page: number;
    pageSize: number;
  } & Partial<PermissionQuery>
) => {
  return request<PageDTO<PermissionVO>>({ url: '/api/v1/auth/permissions', method: 'get', params });
};

/** 获取权限下拉列表 */
export const getPermissionOptions = (query?: PermissionQuery) => {
  return request<PermissionVO[]>({
    url: '/api/v1/auth/permissions/options',
    method: 'get',
    params: query as Record<string, unknown> | undefined
  });
};

/** 新增权限 */
export const createPermission = (data: PermissionCreateRequestDTO) => {
  return request<boolean>({ url: '/api/v1/auth/permissions', method: 'post', data });
};

/** 更新权限 */
export const updatePermission = (id: string, data: PermissionUpdateRequestDTO) => {
  return request<boolean>({ url: `/api/v1/auth/permissions/${id}`, method: 'put', data });
};

/** 删除权限 */
export const deletePermission = (id: string) => {
  return request<boolean>({ url: `/api/v1/auth/permissions/${id}`, method: 'delete' });
};
