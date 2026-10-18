import { request } from '@/service/request';

/** 给加工中的 Lot 录入一条原始数值。 */
export const recordCollect = (data: { lotId: string; itemId: string; valueNum: number }) => {
  return request<boolean>({ url: '/api/v1/mes/collect-results', method: 'post', data });
};

/** 更正当前经过的有效值。 */
export const correctCollect = (data: { lotId: string; itemId: string; valueNum: number }) => {
  return request<boolean>({ url: '/api/v1/mes/collect-results/correct', method: 'post', data });
};

/** 作废当前经过的最后一条采集值。 */
export const voidCollect = (data: { lotId: string; itemId: string }) => {
  return request<boolean>({ url: '/api/v1/mes/collect-results/void', method: 'post', data });
};

/** 查询某个步骤、某次经过的有效值。没有或已作废时为空。 */
export const getEffectiveCollect = (params: { lotId: string; itemId: string; routeSeqNo: number; passNo: number }) => {
  return request<number | null>({ url: '/api/v1/mes/collect-results/effective', method: 'get', params });
};
