import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';
import type { ProductVersion, ProductVersionSaveRequest } from './types';

export type { ProductVersion, ProductVersionSaveRequest } from './types';

/** 分页查询某个产品的版本。 */
export const getProductVersionList = (productId: string, params: { page: number; pageSize: number }) => {
  return request<PageDTO<ProductVersion>>({ url: `/api/v1/mes/products/${productId}/versions`, method: 'get', params });
};

/** 新增草稿。 */
export const createProductVersion = (productId: string, data: ProductVersionSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/products/${productId}/versions`, method: 'post', data });
};

/** 修改草稿。 */
export const updateProductVersion = (productId: string, id: string, data: ProductVersionSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/products/${productId}/versions/${id}`, method: 'put', data });
};

/** 复制成新草稿。 */
export const copyProductVersion = (productId: string, id: string) => {
  return request<boolean>({ url: `/api/v1/mes/products/${productId}/versions/${id}/copy`, method: 'post' });
};

/** 提交审批。assignee 是指定审批人。 */
export const submitProductVersion = (productId: string, id: string, data: { assignee: string; comment?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/products/${productId}/versions/${id}/submit`, method: 'post', data });
};

/** 审批通过并生效。operator 必须是指定审批人。 */
export const approveProductVersion = (productId: string, id: string, data: { operator: string; comment?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/products/${productId}/versions/${id}/approve`, method: 'post', data });
};

/** 驳回为草稿。 */
export const rejectProductVersion = (productId: string, id: string, data: { operator: string; comment?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/products/${productId}/versions/${id}/reject`, method: 'post', data });
};
