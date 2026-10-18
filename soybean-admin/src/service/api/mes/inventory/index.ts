import { request } from '@/service/request';

export interface InventoryRow {
  id: string;
  materialLotId: string;
  materialId: string;
  locationId: string;
  qtyOnHand: number;
  qtyFrozen: number;
  uomId: string;
}

/** 列出库存。 */
export const getInventories = () => {
  return request<InventoryRow[]>({ url: '/api/v1/mes/inventories', method: 'get' });
};
