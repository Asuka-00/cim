import { request } from '@/service/request';

/** 等待中的主流程 Lot 改到新 Route 的指定步骤。返回被取消的旧预约条数。 */
export const changeLotRoute = (
  lotId: string,
  data: { routeVersionId: string; routeSeqNo: number; reasonCode: string; productVersionId?: string | null }
) => {
  return request<number>({ url: `/api/v1/mes/lots/${lotId}/route`, method: 'post', data });
};
