import { request } from '@/service/request';
import type { RouteTimeLink, RouteTimeLinkSaveRequest } from './types';

export type { RouteTimeLink, RouteTimeLinkSaveRequest } from './types';

/** 列出某个版本的 Time Link。 */
export const getRouteTimeLinkList = (routeVersionId: string) => {
  return request<RouteTimeLink[]>({ url: `/api/v1/mes/route-versions/${routeVersionId}/time-links`, method: 'get' });
};

/** 新增 Time Link。只有草稿版本可以改。 */
export const createRouteTimeLink = (routeVersionId: string, data: RouteTimeLinkSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/route-versions/${routeVersionId}/time-links`, method: 'post', data });
};

/** 修改 Time Link。 */
export const updateRouteTimeLink = (routeVersionId: string, id: string, data: RouteTimeLinkSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/route-versions/${routeVersionId}/time-links/${id}`, method: 'put', data });
};

/** 删除 Time Link。 */
export const deleteRouteTimeLink = (routeVersionId: string, id: string) => {
  return request<boolean>({ url: `/api/v1/mes/route-versions/${routeVersionId}/time-links/${id}`, method: 'delete' });
};
