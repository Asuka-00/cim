import { request } from '@/service/request';

/** 给在制 Lot 补入数量。有工单时不能超过计划量。 */
export const bonusLot = (lotId: string, data: { qty: number }) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/bonus`, method: 'post', data });
};
