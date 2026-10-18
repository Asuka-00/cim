import { request } from '@/service/request';

export interface SamplingRule {
  id: string;
  ruleCode: string;
  ruleName: string;
  sampleType: string;
  nValue?: number;
  percent?: number;
  waferCount?: number;
  slotList?: string;
  enabled?: number;
}

/** 列出全部抽检规则。 */
export const getSamplingRules = () => {
  return request<SamplingRule[]>({ url: '/api/v1/mes/sampling-rules', method: 'get' });
};

/** 新建规则。EVERY_N_LOT 要间隔，PERCENT 要百分比。 */
export const createSamplingRule = (data: {
  ruleCode: string;
  ruleName: string;
  sampleType: string;
  nValue?: number | null;
  percent?: number | null;
  waferCount?: number | null;
  slotList?: string;
}) => {
  return request<boolean>({ url: '/api/v1/mes/sampling-rules', method: 'post', data });
};
