import { request } from '@/service/request';

export interface StepCollectItem {
  id: string;
  stepId: string;
  itemId: string;
  productId?: string | number;
  lsl?: number | null;
  usl?: number | null;
  autoHoldOnOos?: number;
  holdReasonCode?: string;
  isRequired?: number;
}

/** 列出这个站点的采集规格。 */
export const getStepCollectItems = (stepId: string) => {
  return request<StepCollectItem[]>({ url: '/api/v1/mes/step-collect-items', method: 'get', params: { stepId } });
};

/** 保存规格。至少要有上限或下限。同一站点、采集项和产品不能重复。 */
export const saveStepCollectItem = (data: {
  stepId: string;
  itemId: string;
  productId?: string | null;
  lsl?: number | null;
  usl?: number | null;
  autoHoldOnOos?: number;
  holdReasonCode?: string;
  isRequired?: number;
}) => {
  return request<boolean>({ url: '/api/v1/mes/step-collect-items', method: 'post', data });
};
