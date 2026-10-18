import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';
import type { ProductFamily, ProductFamilySaveRequest } from './types';

export type { ProductFamily, ProductFamilySaveRequest } from './types';

/** 分页查询产品族。 */
export const getProductFamilyList = (params: { page: number; pageSize: number; familyCode?: string }) => {
  return request<PageDTO<ProductFamily>>({ url: '/api/v1/mes/product-families', method: 'get', params });
};

/** 新增产品族。 */
export const createProductFamily = (data: ProductFamilySaveRequest) => {
  return request<boolean>({ url: '/api/v1/mes/product-families', method: 'post', data });
};

/** 修改产品族。 */
export const updateProductFamily = (id: string, data: ProductFamilySaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/product-families/${id}`, method: 'put', data });
};

/** 删除产品族。 */
export const deleteProductFamily = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/product-families/${id}`, method: 'delete' });
};
