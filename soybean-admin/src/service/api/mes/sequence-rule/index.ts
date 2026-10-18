import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';
import type { SequenceRule, SequenceRuleSaveRequest } from './types';

export type { SequenceRule, SequenceRuleSaveRequest } from './types';

/** 分页查询编号规则。 */
export const getSequenceRuleList = (params: { page: number; pageSize: number; ruleCode?: string }) => {
  return request<PageDTO<SequenceRule>>({ url: '/api/v1/mes/sequence-rules', method: 'get', params });
};

/** 新增编号规则。 */
export const createSequenceRule = (data: SequenceRuleSaveRequest) => {
  return request<boolean>({ url: '/api/v1/mes/sequence-rules', method: 'post', data });
};

/** 修改编号规则。 */
export const updateSequenceRule = (id: string, data: SequenceRuleSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/sequence-rules/${id}`, method: 'put', data });
};

/** 删除编号规则。 */
export const deleteSequenceRule = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/sequence-rules/${id}`, method: 'delete' });
};

/** 预览下一个号码，不消耗流水。 */
export const previewSequenceRule = (ruleCode: string) => {
  return request<string>({ url: `/api/v1/mes/sequence-rules/${ruleCode}/preview`, method: 'get' });
};
