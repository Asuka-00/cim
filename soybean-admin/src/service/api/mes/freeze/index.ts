import { request } from '@/service/request';

/** 冻结一部分库存。在库数量不变。 */
export const freezeInventory = (data: { materialLotId: string; locationId: string; qty: number }) => {
  return request<boolean>({ url: '/api/v1/mes/inventories/freeze', method: 'post', data });
};

/** 解冻一部分库存。 */
export const unfreezeInventory = (data: { materialLotId: string; locationId: string; qty: number }) => {
  return request<boolean>({ url: '/api/v1/mes/inventories/unfreeze', method: 'post', data });
};
