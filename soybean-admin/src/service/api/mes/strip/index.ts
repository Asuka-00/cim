import { request } from '@/service/request';

export interface Strip {
  id: string;
  stripCode: string;
  lotId?: string;
  status: string;
  unitQty?: number;
  goodQty?: number;
}

/** 列出某个 Lot 的 Strip。 */
export const getStrips = (lotId: string) => {
  return request<Strip[]>({ url: `/api/v1/mes/lots/${lotId}/strips`, method: 'get' });
};

/** 挂一条 Strip 到在制 Lot。 */
export const createStrip = (lotId: string, data: { stripCode: string; unitQty: number; goodQty?: number }) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/strips`, method: 'post', data });
};

/** 把仍为 NORMAL 的 Strip 切筋成型，Lot 主数量改为良品合计。 */
export const convertStrips = (lotId: string, data: { unitId: string }) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/strips/convert`, method: 'post', data });
};
