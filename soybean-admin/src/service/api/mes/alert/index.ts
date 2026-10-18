import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';

export interface MesAlert {
  id: string;
  alertType: string;
  level: string;
  title: string;
  content?: string;
  status: string;
  receiverScope?: string;
  readBy?: string;
}

/** 分页查询预警。状态空表示全部。 */
export const getAlertPage = (params: { page: number; pageSize: number; status?: string }) => {
  return request<PageDTO<MesAlert>>({ url: '/api/v1/mes/alerts', method: 'get', params });
};

/** 标记已读。 */
export const readAlert = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/alerts/${id}/read`, method: 'post' });
};

/** 关闭预警。 */
export const closeAlert = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/alerts/${id}/close`, method: 'post' });
};
