import type {
  SystemConfigCreateRequestDTO,
  SystemConfigQuery,
  SystemConfigUpdateRequestDTO,
  SystemConfigVO
} from './types';
import { request } from '@/service/request';

export type {
  SystemConfigCreateRequestDTO,
  SystemConfigQuery,
  SystemConfigUpdateRequestDTO,
  SystemConfigVO
} from './types';

/** 查询系统配置列表（全量） */
export const getSystemConfigList = (params?: Partial<SystemConfigQuery>) => {
  return request<SystemConfigVO[]>({ url: '/api/v1/system-configs', method: 'get', params });
};

/** 根据ID查询系统配置 */
export const getSystemConfigById = (id: string) => {
  return request<SystemConfigVO>({ url: `/api/v1/system-configs/${id}`, method: 'get' });
};

/** 根据编码查询系统配置 */
export const getSystemConfigByCode = (configCode: string) => {
  return request<SystemConfigVO>({ url: `/api/v1/system-configs/code/${configCode}`, method: 'get' });
};

/** 新增系统配置 */
export const createSystemConfig = (data: SystemConfigCreateRequestDTO) => {
  return request<boolean>({ url: '/api/v1/system-configs', method: 'post', data });
};

/** 更新系统配置 */
export const updateSystemConfig = (id: string, data: SystemConfigUpdateRequestDTO) => {
  return request<boolean>({ url: `/api/v1/system-configs/${id}`, method: 'put', data });
};

/** 删除系统配置 */
export const deleteSystemConfig = (id: string) => {
  return request<boolean>({ url: `/api/v1/system-configs/${id}`, method: 'delete' });
};
