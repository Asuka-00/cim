import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';
import type { StepGroup, StepGroupSaveRequest } from './types';

export type { StepGroup, StepGroupSaveRequest } from './types';

/** 分页查询站点组，供站点选择。 */
export const getStepGroupList = (params: { page: number; pageSize: number; groupCode?: string }) => {
  return request<PageDTO<StepGroup>>({ url: '/api/v1/mes/step-groups', method: 'get', params });
};

/** 新增站点组。 */
export const createStepGroup = (data: StepGroupSaveRequest) => {
  return request<boolean>({ url: '/api/v1/mes/step-groups', method: 'post', data });
};

/** 修改站点组。 */
export const updateStepGroup = (id: string, data: StepGroupSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/step-groups/${id}`, method: 'put', data });
};

/** 删除站点组。 */
export const deleteStepGroup = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/step-groups/${id}`, method: 'delete' });
};
