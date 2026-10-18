import { request } from '@/service/request';

/** 进站。站点必须是 Lot 当前步骤。 */
export const trackInLot = (
  lotId: string,
  data: { stepId: string; equipmentId?: string | null }
) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/track-in`, method: 'post', data });
};

/** 出站。备注可空。 */
export const trackOutLot = (lotId: string, data: { remark?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/track-out`, method: 'post', data });
};
