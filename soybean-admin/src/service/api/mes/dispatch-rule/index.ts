import { request } from '@/service/request';

export interface DispatchRule {
  id: string;
  ruleCode: string;
  ruleName: string;
  sortKeys?: string;
  isDefault?: number;
  enabled?: number;
}

/** 列出全部排序规则。 */
export const getDispatchRules = () => {
  return request<DispatchRule[]>({ url: '/api/v1/mes/dispatch-rules', method: 'get' });
};

/** 新建全局规则。排序键形如 IS_HOT:DESC,PRIORITY:ASC。 */
export const createDispatchRule = (data: { ruleCode: string; ruleName: string; sortKeys: string; isDefault?: number }) => {
  return request<boolean>({ url: '/api/v1/mes/dispatch-rules', method: 'post', data });
};
