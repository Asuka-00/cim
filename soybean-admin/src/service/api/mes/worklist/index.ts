import { request } from '@/service/request';

export interface WorklistItem {
  lotId: string;
  lotNo: string;
  priority?: number;
  isHot?: number;
  stepId?: string;
  stepEnterAt?: string;
  qty?: number;
}

/** 查询未 Hold 的等待 Lot。不传条件时不限站点、区域和设备，排序用默认规则。 */
export const getWorklist = (params: { stepId?: string; areaId?: string; equipmentId?: string; ruleId?: string }) => {
  return request<WorklistItem[]>({ url: '/api/v1/mes/worklist', method: 'get', params });
};
