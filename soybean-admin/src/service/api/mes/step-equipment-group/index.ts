import { request } from '@/service/request';
import type { StepEquipmentGroup, StepEquipmentGroupSaveRequest } from './types';

export type { StepEquipmentGroup, StepEquipmentGroupSaveRequest } from './types';

/** 列出某个站点的设备组绑定，按优先级从小到大。 */
export const getStepEquipmentGroupList = (stepId: string) => {
  return request<StepEquipmentGroup[]>({ url: '/api/v1/mes/step-equipment-groups', method: 'get', params: { stepId } });
};

/** 新增绑定。 */
export const createStepEquipmentGroup = (data: StepEquipmentGroupSaveRequest) => {
  return request<boolean>({ url: '/api/v1/mes/step-equipment-groups', method: 'post', data });
};

/** 修改绑定。 */
export const updateStepEquipmentGroup = (id: string, data: StepEquipmentGroupSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/step-equipment-groups/${id}`, method: 'put', data });
};

/** 删除绑定。 */
export const deleteStepEquipmentGroup = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/step-equipment-groups/${id}`, method: 'delete' });
};
