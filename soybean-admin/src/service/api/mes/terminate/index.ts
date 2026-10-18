import { request } from '@/service/request';

/** 把在制 Lot 终止。数量保留，Hold 不阻止。 */
export const terminateLot = (lotId: string, data: { reasonCode: string; remark?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/terminate`, method: 'post', data });
};
