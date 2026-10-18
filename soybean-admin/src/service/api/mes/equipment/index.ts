import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';
import type { Equipment, EquipmentSaveRequest, EquipmentStatusChangeRequest, EquipmentStatusHist } from './types';

export type { Equipment, EquipmentSaveRequest, EquipmentStatusChangeRequest, EquipmentStatusHist } from './types';

/** 分页查询设备。 */
export const getEquipmentList = (params: { page: number; pageSize: number; equipmentCode?: string }) => {
  return request<PageDTO<Equipment>>({ url: '/api/v1/mes/equipment', method: 'get', params });
};

/** 新增设备。新建后当前状态为 STANDBY。 */
export const createEquipment = (data: EquipmentSaveRequest) => {
  return request<boolean>({ url: '/api/v1/mes/equipment', method: 'post', data });
};

/** 修改设备台账，不改当前状态。 */
export const updateEquipment = (id: string, data: EquipmentSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/equipment/${id}`, method: 'put', data });
};

/** 删除设备。 */
export const deleteEquipment = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/equipment/${id}`, method: 'delete' });
};

/** 手工切换设备状态。 */
export const changeEquipmentStatus = (id: string, data: EquipmentStatusChangeRequest) => {
  return request<boolean>({ url: `/api/v1/mes/equipment/${id}/status`, method: 'post', data });
};

/** 分页查询设备状态历史。 */
export const getEquipmentStatusHistory = (id: string, params: { page: number; pageSize: number }) => {
  return request<PageDTO<EquipmentStatusHist>>({ url: `/api/v1/mes/equipment/${id}/status-history`, method: 'get', params });
};
