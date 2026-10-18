import { request } from '@/service/request';

export interface WaferHistory {
  id: string;
  waferCode?: string;
  lotNo?: string;
  txnType?: string;
  routeSeqNo?: number;
  passNo?: number;
  slotNoBefore?: number;
  slotNoAfter?: number;
  statusBefore?: string;
  statusAfter?: string;
  reasonCode?: string;
  operator?: string;
  occurredAt?: string;
  remark?: string;
}

/** 查询一片晶圆的历史，新的在前。只含片自身变化，不含整批过站。 */
export const getWaferHistory = (waferId: string) => {
  return request<WaferHistory[]>({ url: `/api/v1/mes/wafers/${waferId}/history`, method: 'get' });
};
