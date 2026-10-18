import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';
import type { Bom, BomSaveRequest } from './types';

export type { Bom, BomSaveRequest } from './types';

/** 分页查询 BOM。 */
export const getBomList = (params: { page: number; pageSize: number; bomCode?: string }) => {
  return request<PageDTO<Bom>>({ url: '/api/v1/mes/boms', method: 'get', params });
};

/** 新增 BOM。 */
export const createBom = (data: BomSaveRequest) => {
  return request<boolean>({ url: '/api/v1/mes/boms', method: 'post', data });
};

/** 修改 BOM。 */
export const updateBom = (id: string, data: BomSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/boms/${id}`, method: 'put', data });
};

/** 删除 BOM。 */
export const deleteBom = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/boms/${id}`, method: 'delete' });
};
