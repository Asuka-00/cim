import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';
import type { EquipmentGroup, EquipmentGroupSaveRequest } from './types';

export type { EquipmentGroup, EquipmentGroupSaveRequest } from './types';

/** 分页查询设备组。 */
export const getEquipmentGroupList = (params: { page: number; pageSize: number; groupCode?: string }) => {
  return request<PageDTO<EquipmentGroup>>({ url: '/api/v1/mes/equipment-groups', method: 'get', params });
};

/** 新增设备组。 */
export const createEquipmentGroup = (data: EquipmentGroupSaveRequest) => {
  return request<boolean>({ url: '/api/v1/mes/equipment-groups', method: 'post', data });
};

/** 修改设备组。 */
export const updateEquipmentGroup = (id: string, data: EquipmentGroupSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/equipment-groups/${id}`, method: 'put', data });
};

/** 删除设备组。 */
export const deleteEquipmentGroup = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/equipment-groups/${id}`, method: 'delete' });
};
