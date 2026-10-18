import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';
import type { Step, StepSaveRequest } from './types';

export type { Step, StepSaveRequest } from './types';

/** 分页查询站点。 */
export const getStepList = (params: { page: number; pageSize: number; stepCode?: string }) => {
  return request<PageDTO<Step>>({ url: '/api/v1/mes/steps', method: 'get', params });
};

/** 新增站点。 */
export const createStep = (data: StepSaveRequest) => {
  return request<boolean>({ url: '/api/v1/mes/steps', method: 'post', data });
};

/** 修改站点。 */
export const updateStep = (id: string, data: StepSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/steps/${id}`, method: 'put', data });
};

/** 删除站点。 */
export const deleteStep = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/steps/${id}`, method: 'delete' });
};
