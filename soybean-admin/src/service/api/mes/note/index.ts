import { request } from '@/service/request';

export interface LotNote {
  id: string;
  lotId: string;
  noteType: string;
  content: string;
  pinned?: number;
}

/** 查看 Lot 备注。置顶在前。 */
export const getLotNotes = (lotId: string) => {
  return request<LotNote[]>({ url: `/api/v1/mes/lots/${lotId}/notes`, method: 'get' });
};

/** 追加备注。类型空则按 NOTE。 */
export const addLotNote = (lotId: string, data: { content: string; noteType?: string; pinned?: number }) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/notes`, method: 'post', data });
};
