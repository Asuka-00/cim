import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';

export interface ApprovalRecord {
  id: string;
  bizType?: string;
  bizId?: string;
  action?: string;
  fromStatus?: string;
  toStatus?: string;
  assignee?: string;
  operator?: string;
  comment?: string;
  actionAt?: string;
}

/** 按业务对象分页查询审批记录。 */
export const getApprovalRecords = (params: { bizType: string; bizId: string; page: number; pageSize: number }) => {
  return request<PageDTO<ApprovalRecord>>({ url: '/api/v1/mes/approval-records', method: 'get', params });
};
