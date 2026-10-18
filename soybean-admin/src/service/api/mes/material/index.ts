import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';
import type { Material, MaterialSaveRequest } from './types';

export type { Material, MaterialSaveRequest } from './types';

/** 分页查询物料。 */
export const getMaterialList = (params: { page: number; pageSize: number; materialCode?: string }) => {
  return request<PageDTO<Material>>({ url: '/api/v1/mes/materials', method: 'get', params });
};

/** 新增物料。 */
export const createMaterial = (data: MaterialSaveRequest) => {
  return request<boolean>({ url: '/api/v1/mes/materials', method: 'post', data });
};

/** 修改物料。 */
export const updateMaterial = (id: string, data: MaterialSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/materials/${id}`, method: 'put', data });
};

/** 删除物料。 */
export const deleteMaterial = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/materials/${id}`, method: 'delete' });
};
