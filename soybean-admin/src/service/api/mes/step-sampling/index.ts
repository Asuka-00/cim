import { request } from '@/service/request';

export interface StepSampling {
  id: string;
  stepId: string;
  productId?: string | number;
  ruleId: string;
}

/** 列出站点抽检绑定。产品为 0 表示不限产品。 */
export const getStepSamplings = () => {
  return request<StepSampling[]>({ url: '/api/v1/mes/step-samplings', method: 'get' });
};

/** 把已启用的规则绑到站点。产品留空表示不限产品。 */
export const bindStepSampling = (data: { stepId: string; productId?: string | null; ruleId: string }) => {
  return request<boolean>({ url: '/api/v1/mes/step-samplings', method: 'post', data });
};
