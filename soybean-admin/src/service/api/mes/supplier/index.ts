import { request } from '@/service/request';

export interface Supplier {
  id: string;
  supplierCode: string;
  supplierName: string;
  contact?: string;
  phone?: string;
  enabled: number;
}

/** 列出供应商。 */
export const getSuppliers = () => {
  return request<Supplier[]>({ url: '/api/v1/mes/suppliers', method: 'get' });
};

/** 新建供应商。 */
export const createSupplier = (data: { supplierCode: string; supplierName: string; contact?: string; phone?: string }) => {
  return request<boolean>({ url: '/api/v1/mes/suppliers', method: 'post', data });
};
