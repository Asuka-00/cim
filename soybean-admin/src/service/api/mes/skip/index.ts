import { request } from '@/service/request';

/** 等待中的 Lot 跳到下一步。 */
export const skipLot = (lotId: string, data: { reasonCode: string; remark?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/skip`, method: 'post', data });
};
