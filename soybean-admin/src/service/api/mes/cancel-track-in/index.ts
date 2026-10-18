import { request } from '@/service/request';

/** 加工中的 Lot 取消进站，回到等待并离开设备。 */
export const cancelTrackIn = (lotId: string, data: { reasonCode: string; remark?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/cancel-track-in`, method: 'post', data });
};
