import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';
import type { Lot, LotStartRequest } from './types';

export type { Lot, LotStartRequest } from './types';

/** 分页查询 Lot。 */
export const getLotList = (params: { page: number; pageSize: number; lotNo?: string; status?: string }) => {
  return request<PageDTO<Lot>>({ url: '/api/v1/mes/lots', method: 'get', params });
};

/** 按主键查询一批。不存在会报错。 */
export const getLot = (id: string) => {
  return request<Lot>({ url: `/api/v1/mes/lots/${id}`, method: 'get' });
};

/** 从已下达工单开批。 */
export const startLot = (woId: string, data: LotStartRequest) => {
  return request<boolean>({ url: `/api/v1/mes/work-orders/${woId}/lots`, method: 'post', data });
};

/** 往已下达工单导入一条期初在制。 */
export const importWipLot = (
  woId: string,
  data: { lotNo: string; qty: number; seqNo: number; stepId: string; passCount?: number }
) => {
  return request<boolean>({ url: `/api/v1/mes/work-orders/${woId}/lots/import`, method: 'post', data });
};
