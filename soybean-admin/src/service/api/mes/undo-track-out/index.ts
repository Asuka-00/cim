import { request } from '@/service/request';

/** 按最近一次出站前的快照回到加工中。 */
export const undoTrackOut = (lotId: string, data: { reasonCode: string; remark?: string; toolId?: string | null }) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/undo-track-out`, method: 'post', data });
};
