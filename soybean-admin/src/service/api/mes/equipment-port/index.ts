import { request } from '@/service/request';

export interface EquipmentPort {
  id: string;
  equipmentId: string;
  portCode: string;
  portName?: string;
  kind: string;
  maxConcurrent?: number;
  e10State: string;
  enabled?: number;
}

/** 列出这台设备的端口。 */
export const getEquipmentPorts = (equipmentId: string) => {
  return request<EquipmentPort[]>({ url: `/api/v1/mes/equipment/${equipmentId}/ports`, method: 'get' });
};

/** 设备抽屉沿用的列表名。 */
export const getEquipmentPortList = getEquipmentPorts;

/** 手工切换腔体状态。停机类必须带原因码。 */
export const changeChamberStatus = (
  equipmentId: string,
  portId: string,
  data: { e10State: string; reasonCode?: string }
) => {
  return request<boolean>({
    url: `/api/v1/mes/equipment/${equipmentId}/ports/${portId}/status`,
    method: 'post',
    data
  });
};

/** 新增端口。类型空按 PORT，同时加工上限空按 1。 */
export const createEquipmentPort = (
  equipmentId: string,
  data: { portCode: string; portName?: string; kind?: string; maxConcurrent?: number | null; enabled?: number }
) => {
  return request<boolean>({ url: `/api/v1/mes/equipment/${equipmentId}/ports`, method: 'post', data });
};

/** 修改端口台账。当前状态不在这里改。 */
export const updateEquipmentPort = (
  equipmentId: string,
  id: string,
  data: { portCode: string; portName?: string; kind?: string; maxConcurrent?: number | null; enabled?: number }
) => {
  return request<boolean>({ url: `/api/v1/mes/equipment/${equipmentId}/ports/${id}`, method: 'put', data });
};

/** 删除端口。 */
export const deleteEquipmentPort = (equipmentId: string, id: string) => {
  return request<boolean>({ url: `/api/v1/mes/equipment/${equipmentId}/ports/${id}`, method: 'delete' });
};
