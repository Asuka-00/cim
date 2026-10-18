import { request } from '@/service/request';
import type { RouteStep, RouteStepSaveRequest } from './types';

export type { RouteStep, RouteStepSaveRequest } from './types';

/** 列出某个版本的步骤，按序号从小到大。 */
export const getRouteStepList = (routeVersionId: string) => {
  return request<RouteStep[]>({ url: `/api/v1/mes/route-versions/${routeVersionId}/steps`, method: 'get' });
};

/** 新增步骤。只有草稿版本可以改。 */
export const createRouteStep = (routeVersionId: string, data: RouteStepSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/route-versions/${routeVersionId}/steps`, method: 'post', data });
};

/** 修改步骤。 */
export const updateRouteStep = (routeVersionId: string, id: string, data: RouteStepSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/route-versions/${routeVersionId}/steps/${id}`, method: 'put', data });
};

/** 删除步骤。仍被步骤 Recipe 引用时会拒绝。 */
export const deleteRouteStep = (routeVersionId: string, id: string) => {
  return request<boolean>({ url: `/api/v1/mes/route-versions/${routeVersionId}/steps/${id}`, method: 'delete' });
};
