import { request } from '@/service/request';
import type { CapabilityDecision, EquipmentCapability, EquipmentCapabilitySaveRequest } from './types';

export type { CapabilityDecision, EquipmentCapability, EquipmentCapabilitySaveRequest } from './types';

/** 列出某台设备的能力。 */
export const getEquipmentCapabilityList = (equipmentId: string) => {
  return request<EquipmentCapability[]>({ url: '/api/v1/mes/equipment-capabilities', method: 'get', params: { equipmentId } });
};

/** 新增能力。 */
export const createEquipmentCapability = (data: EquipmentCapabilitySaveRequest) => {
  return request<boolean>({ url: '/api/v1/mes/equipment-capabilities', method: 'post', data });
};

/** 修改能力。 */
export const updateEquipmentCapability = (id: string, data: EquipmentCapabilitySaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/equipment-capabilities/${id}`, method: 'put', data });
};

/** 删除能力。 */
export const deleteEquipmentCapability = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/equipment-capabilities/${id}`, method: 'delete' });
};

/** 判定这台设备能否加工。没有能力记录时，groupBound 为真才按设备组放行。 */
export const decideEquipmentCapability = (params: {
  equipmentId: string;
  stepId: string;
  recipeId?: string;
  productId?: string;
  groupBound: boolean;
}) => {
  return request<CapabilityDecision>({ url: '/api/v1/mes/equipment-capabilities/decide', method: 'get', params });
};
