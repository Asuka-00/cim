import { request } from '@/service/request';

export interface Handover {
  id: string;
  handoverNo: string;
  fromUser: string;
  toUser?: string;
  status: string;
  remark?: string;
  contentJson?: string;
}

/** 列出全部交接班。 */
export const getHandovers = () => {
  return request<Handover[]>({ url: '/api/v1/mes/handovers', method: 'get' });
};

/** 新建草稿。 */
export const createHandover = (data: { handoverNo: string; fromUser: string; remark?: string }) => {
  return request<boolean>({ url: '/api/v1/mes/handovers', method: 'post', data });
};

/** 提交草稿，并写入当时的在制和异常快照。 */
export const submitHandover = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/handovers/${id}/submit`, method: 'post' });
};

/** 接班人确认已提交的交接班。 */
export const confirmHandover = (id: string, data: { toUser: string }) => {
  return request<boolean>({ url: `/api/v1/mes/handovers/${id}/confirm`, method: 'post', data });
};
