import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';
import type { ProductUnitConv, ProductUnitConvCheck, ProductUnitConvSaveRequest } from './types';

export type { ProductUnitConv, ProductUnitConvCheck, ProductUnitConvSaveRequest } from './types';

/** 分页查询某个产品的计数换算。 */
export const getProductUnitConvList = (productId: string, params: { page: number; pageSize: number }) => {
  return request<PageDTO<ProductUnitConv>>({
    url: `/api/v1/mes/products/${productId}/unit-conversions`,
    method: 'get',
    params
  });
};

/** 按系数核对实际数量。 */
export const checkProductUnitConv = (
  productId: string,
  params: { fromUnitId: string; toUnitId: string; fromQty: number; actualQty: number }
) => {
  return request<ProductUnitConvCheck>({
    url: `/api/v1/mes/products/${productId}/unit-conversions/check`,
    method: 'get',
    params
  });
};

/** 新增产品计数换算。 */
export const createProductUnitConv = (productId: string, data: ProductUnitConvSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/products/${productId}/unit-conversions`, method: 'post', data });
};

/** 修改产品计数换算。 */
export const updateProductUnitConv = (productId: string, id: string, data: ProductUnitConvSaveRequest) => {
  return request<boolean>({
    url: `/api/v1/mes/products/${productId}/unit-conversions/${id}`,
    method: 'put',
    data
  });
};

/** 删除产品计数换算。 */
export const deleteProductUnitConv = (productId: string, id: string) => {
  return request<boolean>({ url: `/api/v1/mes/products/${productId}/unit-conversions/${id}`, method: 'delete' });
};
