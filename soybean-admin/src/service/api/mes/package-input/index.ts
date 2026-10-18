import { request } from '@/service/request';

/** 从源 Lot 扣数量加到封装 Lot。源 Lot 扣到 0 时变为 CONSUMED。 */
export const inputPackage = (lotId: string, data: { sourceLotId: string; qty: number }) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/package-input`, method: 'post', data });
};
