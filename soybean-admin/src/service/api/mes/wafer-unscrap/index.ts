import { request } from '@/service/request';

/** 把报废片恢复到报废前的状态，数量加回所属 Lot。 */
export const unscrapWafer = (waferId: string) => {
  return request<boolean>({ url: `/api/v1/mes/wafers/${waferId}/unscrap`, method: 'post' });
};
