import type { PageDTO } from '@/service/api/common';
import type { TenantCreateRequestDTO, TenantQuery, TenantSelectVO, TenantUpdateRequestDTO, TenantVO } from './types';
import { request } from '@/service/request';

export type { TenantCreateRequestDTO, TenantQuery, TenantSelectVO, TenantUpdateRequestDTO, TenantVO } from './types';

/** 分页查询租户列表 */
export const getTenantList = (
  params: {
    page: number;
    pageSize: number;
  } & Partial<TenantQuery>
) => {
  return request<PageDTO<TenantVO>>({ url: '/api/v1/tenants', method: 'get', params });
};

/** 根据ID查询租户 */
export const getTenantById = (id: string) => {
  return request<TenantVO>({ url: `/api/v1/tenants/${id}`, method: 'get' });
};

/** 根据租户ID查询租户 */
export const getTenantByTenantId = (tenantId: string) => {
  return request<TenantVO>({ url: `/api/v1/tenants/tenant-id/${tenantId}`, method: 'get' });
};

/** 获取租户下拉列表 */
export const getTenantDropdown = () => {
  return request<TenantSelectVO[]>({ url: '/api/v1/tenants/dropdown', method: 'get' });
};

/** 新增租户 */
export const createTenant = (data: TenantCreateRequestDTO) => {
  return request<boolean>({ url: '/api/v1/tenants', method: 'post', data });
};

/** 更新租户 */
export const updateTenant = (id: string, data: TenantUpdateRequestDTO) => {
  return request<boolean>({ url: `/api/v1/tenants/${id}`, method: 'put', data });
};

/** 删除租户 */
export const deleteTenant = (id: string) => {
  return request<boolean>({ url: `/api/v1/tenants/${id}`, method: 'delete' });
};
