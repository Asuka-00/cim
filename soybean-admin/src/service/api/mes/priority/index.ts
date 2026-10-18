import { request } from '@/service/request';

/** 修改在制 Lot 的优先级。1 最高，9 最低。 */
export const changeLotPriority = (lotId: string, data: { priority: number }) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/priority`, method: 'post', data });
};
