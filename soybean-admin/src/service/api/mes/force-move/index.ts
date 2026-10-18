import { request } from '@/service/request';

/** 等待中的主流程 Lot 跳到当前 Route 上的指定步骤。 */
export const forceMoveLot = (lotId: string, data: { routeSeqNo: number; reasonCode: string; remark?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/force-move`, method: 'post', data });
};
