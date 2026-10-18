import type { PageDTO } from '@/service/api/common';
import type { OperationLogQuery, OperationLogVO } from './types';
import { request } from '@/service/request';

export type { OperationLogQuery, OperationLogVO } from './types';

/** 分页查询操作日志 */
export const getOperationLogList = (
  params: {
    page: number;
    pageSize: number;
  } & Partial<OperationLogQuery>
) => {
  return request<PageDTO<OperationLogVO>>({ url: '/api/v1/audit/operation-logs', method: 'get', params });
};
