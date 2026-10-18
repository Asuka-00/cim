import { request } from '@/service/request';

export interface EquipmentFault {
  id: string;
  faultNo: string;
  equipmentId: string;
  severity?: string;
  description?: string;
  status: string;
  solution?: string;
  affectedLotCount?: number;
}

/** 列出全部设备异常。 */
export const getEquipmentFaults = () => {
  return request<EquipmentFault[]>({ url: '/api/v1/mes/equipment-faults', method: 'get' });
};

/** 登记异常。勾选停机时才把设备切到非计划停机。 */
export const openEquipmentFault = (data: {
  faultNo: string;
  equipmentId: string;
  description: string;
  severity?: string;
  lotIds?: string[];
  downEquipment?: boolean;
}) => {
  return request<boolean>({ url: '/api/v1/mes/equipment-faults', method: 'post', data });
};

/** 关闭未关闭的异常。 */
export const closeEquipmentFault = (faultId: string, data: { solution: string }) => {
  return request<boolean>({ url: `/api/v1/mes/equipment-faults/${faultId}/close`, method: 'post', data });
};
