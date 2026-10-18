import { request } from '@/service/request';

export interface WipStep {
  stepId?: string | null;
  lotCount: number;
  qty: number;
  holdCount: number;
}

/** 按站点统计等待和加工中的 Lot。完成、合批结束和终止的批不计入。 */
export const getWipByStep = () => {
  return request<WipStep[]>({ url: '/api/v1/mes/reports/wip-by-step', method: 'get' });
};
