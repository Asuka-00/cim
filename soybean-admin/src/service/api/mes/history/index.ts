import { request } from '@/service/request';

export interface LotHistory {
  id: string;
  lotNo?: string;
  txnType: string;
  routeSeqNo?: number;
  statusBefore?: string;
  statusAfter?: string;
  qtyBefore?: number;
  qtyAfter?: number;
  reasonCode?: string;
  operator?: string;
  occurredAt?: string;
  remark?: string;
}

/** 查看 Lot 历史。新的在前。 */
export const getLotHistory = (lotId: string) => {
  return request<LotHistory[]>({ url: `/api/v1/mes/lots/${lotId}/history`, method: 'get' });
};
