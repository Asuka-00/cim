import type { PageDTO } from '@/service/api/common';
import type { DictItemCreateRequestDTO, DictItemQuery, DictItemUpdateRequestDTO, DictItemVO } from './types';
import { request } from '@/service/request';

export type { DictItemCreateRequestDTO, DictItemQuery, DictItemUpdateRequestDTO, DictItemVO } from './types';

/** 分页查询字典选项列表 */
export const getDictItemList = (
  params: {
    page: number;
    pageSize: number;
  } & Partial<DictItemQuery>
) => {
  return request<PageDTO<DictItemVO>>({ url: '/api/v1/system/dict-items', method: 'get', params });
};

/** 根据ID查询字典选项 */
export const getDictItemById = (id: string) => {
  return request<DictItemVO>({ url: `/api/v1/system/dict-items/${id}`, method: 'get' });
};

/** 按字典类型查询启用状态的选项 */
export const getEnabledDictItems = (dictType: string) => {
  return request<DictItemVO[]>({ url: `/api/v1/system/dict-items/type/${dictType}/enabled`, method: 'get' });
};

/** 新增字典选项 */
export const createDictItem = (data: DictItemCreateRequestDTO) => {
  return request<boolean>({ url: '/api/v1/system/dict-items', method: 'post', data });
};

/** 更新字典选项 */
export const updateDictItem = (id: string, data: DictItemUpdateRequestDTO) => {
  return request<boolean>({ url: `/api/v1/system/dict-items/${id}`, method: 'put', data });
};

/** 删除字典选项 */
export const deleteDictItem = (id: string) => {
  return request<boolean>({ url: `/api/v1/system/dict-items/${id}`, method: 'delete' });
};
