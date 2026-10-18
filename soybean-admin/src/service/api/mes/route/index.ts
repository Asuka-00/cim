import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';
import type { Route, RouteSaveRequest } from './types';

export type { Route, RouteSaveRequest } from './types';

/** 分页查询 Route。 */
export const getRouteList = (params: { page: number; pageSize: number; routeCode?: string }) => {
  return request<PageDTO<Route>>({ url: '/api/v1/mes/routes', method: 'get', params });
};

/** 新增 Route。 */
export const createRoute = (data: RouteSaveRequest) => {
  return request<boolean>({ url: '/api/v1/mes/routes', method: 'post', data });
};

/** 修改 Route。 */
export const updateRoute = (id: string, data: RouteSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/routes/${id}`, method: 'put', data });
};

/** 删除 Route。 */
export const deleteRoute = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/routes/${id}`, method: 'delete' });
};
