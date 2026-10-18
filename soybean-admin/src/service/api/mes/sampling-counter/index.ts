import { request } from '@/service/request';

export interface SamplingCounter {
  id: string;
  ruleId: string;
  stepId: string;
  routeStepId?: string | number;
  productId?: string | number;
  counter?: number;
  lastSampledLotId?: string | null;
}

/** 列出全部抽检计数器。 */
export const getSamplingCounters = () => {
  return request<SamplingCounter[]>({ url: '/api/v1/mes/sampling-counters', method: 'get' });
};

/** 把这一组计数清零。已写下的抽检决定不删除。 */
export const resetSamplingCounter = (data: { ruleId: string; stepId: string; routeStepId?: string | null; productId?: string | null }) => {
  return request<boolean>({ url: '/api/v1/mes/sampling-counters/reset', method: 'post', data });
};
