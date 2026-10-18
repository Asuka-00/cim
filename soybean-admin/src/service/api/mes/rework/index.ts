import { request } from '@/service/request';

/** 等待中的主流程 Lot 进入返工分支。 */
export const enterRework = (lotId: string, data: { reworkId: string; reasonCode?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/rework`, method: 'post', data });
};
