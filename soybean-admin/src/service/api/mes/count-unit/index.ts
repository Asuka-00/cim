import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';
import type { CountUnit, CountUnitSaveRequest } from './types';

export type { CountUnit, CountUnitSaveRequest } from './types';

/** 分页查询计数单位。 */
export const getCountUnitList = (params: { page: number; pageSize: number; unitCode?: string }) => {
  return request<PageDTO<CountUnit>>({ url: '/api/v1/mes/count-units', method: 'get', params });
};

/** 新增计数单位。 */
export const createCountUnit = (data: CountUnitSaveRequest) => {
  return request<boolean>({ url: '/api/v1/mes/count-units', method: 'post', data });
};

/** 修改计数单位。 */
export const updateCountUnit = (id: string, data: CountUnitSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/count-units/${id}`, method: 'put', data });
};

/** 删除计数单位。 */
export const deleteCountUnit = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/count-units/${id}`, method: 'delete' });
};
