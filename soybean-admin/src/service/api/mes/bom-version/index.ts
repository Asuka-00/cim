import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';
import type { BomVersion, BomVersionSaveRequest } from './types';

export type { BomVersion, BomVersionSaveRequest } from './types';

/** 分页查询某个 BOM 的版本。 */
export const getBomVersionList = (bomId: string, params: { page: number; pageSize: number }) => {
  return request<PageDTO<BomVersion>>({ url: `/api/v1/mes/boms/${bomId}/versions`, method: 'get', params });
};

/** 新增草稿。 */
export const createBomVersion = (bomId: string, data: BomVersionSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/boms/${bomId}/versions`, method: 'post', data });
};

/** 修改草稿备注。 */
export const updateBomVersion = (bomId: string, id: string, data: BomVersionSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/boms/${bomId}/versions/${id}`, method: 'put', data });
};

/** 复制版本，并带上全部 BOM 行。 */
export const copyBomVersion = (bomId: string, id: string) => {
  return request<boolean>({ url: `/api/v1/mes/boms/${bomId}/versions/${id}/copy`, method: 'post' });
};

/** 提交审批。 */
export const submitBomVersion = (bomId: string, id: string, data: { assignee: string; comment?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/boms/${bomId}/versions/${id}/submit`, method: 'post', data });
};

/** 审批通过并生效。 */
export const approveBomVersion = (bomId: string, id: string, data: { operator: string; comment?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/boms/${bomId}/versions/${id}/approve`, method: 'post', data });
};

/** 驳回为草稿。 */
export const rejectBomVersion = (bomId: string, id: string, data: { operator: string; comment?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/boms/${bomId}/versions/${id}/reject`, method: 'post', data });
};
