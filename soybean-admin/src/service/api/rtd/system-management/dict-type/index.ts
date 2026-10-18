import type { PageDTO } from '@/service/api/common';
import type { DictTypeCreateRequestDTO, DictTypeQuery, DictTypeUpdateRequestDTO, DictTypeVO } from './types';
import { request } from '@/service/request';

export type { DictTypeCreateRequestDTO, DictTypeQuery, DictTypeUpdateRequestDTO, DictTypeVO } from './types';

/** 分页查询字典类型列表 */
export const getDictTypeList = (
  params: {
    page: number;
    pageSize: number;
  } & Partial<DictTypeQuery>
) => {
  return request<PageDTO<DictTypeVO>>({ url: '/api/v1/system/dict-types', method: 'get', params });
};

/** 查询所有字典类型（用于下拉选择） */
export const getAllDictTypes = () => {
  return request<DictTypeVO[]>({ url: '/api/v1/system/dict-types/all', method: 'get' });
};

/** 根据ID查询字典类型 */
export const getDictTypeById = (id: string) => {
  return request<DictTypeVO>({ url: `/api/v1/system/dict-types/${id}`, method: 'get' });
};

/** 新增字典类型 */
export const createDictType = (data: DictTypeCreateRequestDTO) => {
  return request<boolean>({ url: '/api/v1/system/dict-types', method: 'post', data });
};

/** 更新字典类型 */
export const updateDictType = (id: string, data: DictTypeUpdateRequestDTO) => {
  return request<boolean>({ url: `/api/v1/system/dict-types/${id}`, method: 'put', data });
};

/** 删除字典类型 */
export const deleteDictType = (id: string) => {
  return request<boolean>({ url: `/api/v1/system/dict-types/${id}`, method: 'delete' });
};
