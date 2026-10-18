import type { PageDTO } from '@/service/api/common';
import type { PermissionVO } from '@/service/api/rtd/system-management/permission';
import type {
  ChangePasswordRequestDTO,
  UserCreateRequestDTO,
  UserDetailVO,
  UserQuery,
  UserUpdateRequestDTO,
  UserVO
} from './types';
import { request } from '@/service/request';

export type {
  ChangePasswordRequestDTO,
  UserCreateRequestDTO,
  UserDetailVO,
  UserQuery,
  UserTenantMembership,
  UserUpdateRequestDTO,
  UserVO
} from './types';

/** 分页查询用户列表 */
export const getUserList = (
  params: {
    page: number;
    pageSize: number;
  } & Partial<UserQuery>
) => {
  return request<PageDTO<UserVO>>({ url: '/api/v1/users', method: 'get', params });
};

/** 查询单个用户 */
export const getUser = (userName: string) => {
  return request<UserDetailVO>({ url: `/api/v1/users/${userName}`, method: 'get' });
};

/** 新增用户 */
export const createUser = (data: UserCreateRequestDTO) => {
  return request<boolean>({ url: '/api/v1/users', method: 'post', data });
};

/** 更新用户 */
export const updateUser = (id: string, data: UserUpdateRequestDTO) => {
  return request<boolean>({ url: `/api/v1/users/${id}`, method: 'put', data });
};

/** 删除用户 */
export const deleteUser = (id: string) => {
  return request<boolean>({ url: `/api/v1/users/${id}`, method: 'delete' });
};

/** 重置密码 */
export const resetUserPassword = (userName: string) => {
  return request<boolean>({ url: `/api/v1/users/${userName}/password/reset`, method: 'post' });
};

/** 修改密码 */
export const changePassword = (data: ChangePasswordRequestDTO) => {
  return request<boolean>({ url: '/api/v1/users/password/change', method: 'post', data });
};

/** 获取用户权限 */
export const getUserPermissions = (userName: string) => {
  return request<PermissionVO[]>({ url: `/api/v1/users/${userName}/permissions`, method: 'get' });
};

/** 当前用户可切换的租户 */
export const getMyTenants = () => {
  return request<Api.Auth.TenantSelect[]>({ url: '/api/v1/users/me/tenants', method: 'get' });
};
