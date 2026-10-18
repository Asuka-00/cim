import { request } from '@/service/request';

/** 把报废片，或已终止 Lot 上的片，标成已回收。 */
export const reclaimWafer = (waferId: string) => {
  return request<boolean>({ url: `/api/v1/mes/wafers/${waferId}/reclaim`, method: 'post' });
};

/** 用已回收的片在目标 Lot 上生成新片。新编号不能重复。 */
export const reenterWafer = (lotId: string, data: { reclaimedWaferId: string; waferCode: string }) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/wafers/reenter`, method: 'post', data });
};
