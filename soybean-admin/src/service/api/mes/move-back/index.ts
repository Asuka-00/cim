import { request } from '@/service/request';

/** 等待中的主流程 Lot 退回上一步。不回减已经记过的经过次数。 */
export const moveBackLot = (lotId: string, data: { reasonCode: string; remark?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/move-back`, method: 'post', data });
};
