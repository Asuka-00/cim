import { request } from '@/service/request';

export interface LotConsumption {
  id?: string;
  materialId: string;
  materialLotId: string;
  qty: number;
  source: string;
  shortage: boolean;
}

/** 列出这个 Lot 消耗过的物料。回冲为负数。 */
export const getLotConsumptions = (lotId: string) => {
  return request<LotConsumption[]>({ url: `/api/v1/mes/lots/${lotId}/consumptions`, method: 'get' });
};

/** 线边够扣时补扣这笔欠账。 */
export const settleConsumption = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/consumptions/${id}/settle`, method: 'post' });
};

/** 按修正后的数量追加差额。原记录不改。欠账不能修正。 */
export const adjustConsumption = (id: string, data: { qtyAfter: number }) => {
  return request<boolean>({ url: `/api/v1/mes/consumptions/${id}/adjust`, method: 'post', data });
};
