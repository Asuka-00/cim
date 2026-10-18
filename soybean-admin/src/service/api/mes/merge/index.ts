import { request } from '@/service/request';

/** 把源 Lot 并入目标 Lot。两边都要是未 Hold 的 WAITING。 */
export const mergeLot = (targetLotId: string, data: { sourceLotId: string }) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${targetLotId}/merge`, method: 'post', data });
};
