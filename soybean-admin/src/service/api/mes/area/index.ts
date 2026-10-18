import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';
import type { Area, AreaSaveRequest } from './types';

export type { Area, AreaSaveRequest } from './types';

/** 分页查询区域。 */
export const getAreaList = (params: { page: number; pageSize: number; areaCode?: string }) => {
  return request<PageDTO<Area>>({ url: '/api/v1/mes/areas', method: 'get', params });
};

/** 新增区域。 */
export const createArea = (data: AreaSaveRequest) => {
  return request<boolean>({ url: '/api/v1/mes/areas', method: 'post', data });
};

/** 修改区域。 */
export const updateArea = (id: string, data: AreaSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/areas/${id}`, method: 'put', data });
};

/** 删除区域。 */
export const deleteArea = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/areas/${id}`, method: 'delete' });
};
