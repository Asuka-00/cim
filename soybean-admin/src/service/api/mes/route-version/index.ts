import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';
import type { RouteVersion, RouteVersionSaveRequest } from './types';

export type { RouteVersion, RouteVersionSaveRequest } from './types';

/** 分页查询某个 Route 的版本。 */
export const getRouteVersionList = (routeId: string, params: { page: number; pageSize: number }) => {
  return request<PageDTO<RouteVersion>>({ url: `/api/v1/mes/routes/${routeId}/versions`, method: 'get', params });
};

/** 新增草稿。 */
export const createRouteVersion = (routeId: string, data: RouteVersionSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/routes/${routeId}/versions`, method: 'post', data });
};

/** 修改草稿备注。 */
export const updateRouteVersion = (routeId: string, id: string, data: RouteVersionSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/routes/${routeId}/versions/${id}`, method: 'put', data });
};

/** 复制版本，并带上步骤、Time Link 和步骤 Recipe。 */
export const copyRouteVersion = (routeId: string, id: string) => {
  return request<boolean>({ url: `/api/v1/mes/routes/${routeId}/versions/${id}/copy`, method: 'post' });
};

/** 提交审批。需要设备的步骤必须已有设备组。 */
export const submitRouteVersion = (routeId: string, id: string, data: { assignee: string; comment?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/routes/${routeId}/versions/${id}/submit`, method: 'post', data });
};

/** 审批通过并生效。 */
export const approveRouteVersion = (routeId: string, id: string, data: { operator: string; comment?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/routes/${routeId}/versions/${id}/approve`, method: 'post', data });
};

/** 驳回为草稿。 */
export const rejectRouteVersion = (routeId: string, id: string, data: { operator: string; comment?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/routes/${routeId}/versions/${id}/reject`, method: 'post', data });
};
