import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';
import type { Uom, UomSaveRequest } from './types';

export type { Uom, UomSaveRequest } from './types';

/** 分页查询物料计量单位。 */
export const getUomList = (params: { page: number; pageSize: number; uomCode?: string }) => {
  return request<PageDTO<Uom>>({ url: '/api/v1/mes/uoms', method: 'get', params });
};

/** 新增物料计量单位。 */
export const createUom = (data: UomSaveRequest) => {
  return request<boolean>({ url: '/api/v1/mes/uoms', method: 'post', data });
};

/** 修改物料计量单位。 */
export const updateUom = (id: string, data: UomSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/uoms/${id}`, method: 'put', data });
};

/** 删除物料计量单位。 */
export const deleteUom = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/uoms/${id}`, method: 'delete' });
};
