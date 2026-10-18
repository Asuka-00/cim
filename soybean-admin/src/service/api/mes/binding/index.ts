import { request } from '@/service/request';

export interface EquipmentBinding {
  id: string;
  equipmentId: string;
  materialId: string;
  materialLotId?: string;
  remainingQty?: number;
  status: string;
}

/** 列出设备上的物料绑定。 */
export const getEquipmentBindings = () => {
  return request<EquipmentBinding[]>({ url: '/api/v1/mes/equipment-bindings', method: 'get' });
};

/** 下料。RETURN_LINESIDE 要带库位，SCRAP 不退回库存。 */
export const unmountBinding = (id: string, data: { disposition: string; locationId?: string | null }) => {
  return request<boolean>({ url: `/api/v1/mes/equipment-bindings/${id}/unmount`, method: 'post', data });
};
