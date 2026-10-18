import { request } from '@/service/request';

export interface LotWafer {
  id: string;
  waferCode: string;
  slotNo?: number;
  status?: string;
}

/** 查询 Lot 上的 Wafer。 */
export const getLotWafers = (lotId: string) => {
  return request<LotWafer[]>({ url: `/api/v1/mes/lots/${lotId}/wafers`, method: 'get' });
};

/** 拆出子 Lot。返回子 Lot 号。父 Lot 至少留下一片。 */
export const splitLot = (lotId: string, data: { waferIds: string[] }) => {
  return request<string>({ url: `/api/v1/mes/lots/${lotId}/split`, method: 'post', data });
};
