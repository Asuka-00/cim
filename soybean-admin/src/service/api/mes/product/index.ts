import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';
import type { Product, ProductSaveRequest } from './types';

export type { Product, ProductSaveRequest } from './types';

/** 分页查询产品。 */
export const getProductList = (params: { page: number; pageSize: number; productCode?: string }) => {
  return request<PageDTO<Product>>({ url: '/api/v1/mes/products', method: 'get', params });
};

/** 新增产品。 */
export const createProduct = (data: ProductSaveRequest) => {
  return request<boolean>({ url: '/api/v1/mes/products', method: 'post', data });
};

/** 修改产品。 */
export const updateProduct = (id: string, data: ProductSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/products/${id}`, method: 'put', data });
};

/** 删除产品。 */
export const deleteProduct = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/products/${id}`, method: 'delete' });
};
