import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';
import type { Customer, CustomerSaveRequest } from './types';

export type { Customer, CustomerSaveRequest } from './types';

/** 分页查询客户。 */
export const getCustomerList = (params: { page: number; pageSize: number; customerCode?: string }) => {
  return request<PageDTO<Customer>>({ url: '/api/v1/mes/customers', method: 'get', params });
};

/** 新增客户。 */
export const createCustomer = (data: CustomerSaveRequest) => {
  return request<boolean>({ url: '/api/v1/mes/customers', method: 'post', data });
};

/** 修改客户。 */
export const updateCustomer = (id: string, data: CustomerSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/customers/${id}`, method: 'put', data });
};

/** 删除客户。 */
export const deleteCustomer = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/customers/${id}`, method: 'delete' });
};
