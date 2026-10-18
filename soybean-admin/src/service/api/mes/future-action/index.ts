import { request } from '@/service/request';

export interface FutureAction {
  id: string;
  lotId?: string;
  routeSeqNo?: number;
  passNo?: number | null;
  actionType: string;
  actionParams?: string;
  status: string;
}

/** 列出某个 Lot 的全部预约。 */
export const getFutureActions = (lotId: string) => {
  return request<FutureAction[]>({ url: `/api/v1/mes/lots/${lotId}/future-actions`, method: 'get' });
};

/** 预约到站时 Hold 或改优先级。参数分别是原因码或 1 到 9。 */
export const scheduleFutureAction = (
  lotId: string,
  data: { routeSeqNo: number; passNo?: number | null; actionType: string; actionParams: string }
) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/future-actions`, method: 'post', data });
};
