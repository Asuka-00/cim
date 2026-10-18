import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';
import type { UomConversion, UomConversionSaveRequest } from './types';

export type { UomConversion, UomConversionSaveRequest } from './types';

/** 分页查询物料单位换算。 */
export const getUomConversionList = (params: { page: number; pageSize: number; fromUomId?: string }) => {
  return request<PageDTO<UomConversion>>({ url: '/api/v1/mes/uom-conversions', method: 'get', params });
};

/** 按限定物料优先、通用兜底解析换算。 */
export const resolveUomConversion = (params: { fromUomId: string; toUomId: string; materialId?: string }) => {
  return request<UomConversion>({ url: '/api/v1/mes/uom-conversions/resolve', method: 'get', params });
};

/** 新增物料单位换算。 */
export const createUomConversion = (data: UomConversionSaveRequest) => {
  return request<boolean>({ url: '/api/v1/mes/uom-conversions', method: 'post', data });
};

/** 修改物料单位换算。 */
export const updateUomConversion = (id: string, data: UomConversionSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/uom-conversions/${id}`, method: 'put', data });
};

/** 删除物料单位换算。 */
export const deleteUomConversion = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/uom-conversions/${id}`, method: 'delete' });
};
