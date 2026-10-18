import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';
import type { WorkOrder, WorkOrderSaveRequest } from './types';

export type { WorkOrder, WorkOrderSaveRequest } from './types';

/** 分页查询工单。 */
export const getWorkOrderList = (params: { page: number; pageSize: number; woNo?: string }) => {
  return request<PageDTO<WorkOrder>>({ url: '/api/v1/mes/work-orders', method: 'get', params });
};

/** 创建工单。 */
export const createWorkOrder = (data: WorkOrderSaveRequest) => {
  return request<boolean>({ url: '/api/v1/mes/work-orders', method: 'post', data });
};

/** 下达工单。返回线边不够的物料提示。 */
export const releaseWorkOrder = (id: string) => {
  return request<string[]>({ url: `/api/v1/mes/work-orders/${id}/release`, method: 'post' });
};

/** 取消尚未开批的工单。 */
export const cancelWorkOrder = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/work-orders/${id}/cancel`, method: 'post' });
};
