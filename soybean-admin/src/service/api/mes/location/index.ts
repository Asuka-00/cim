import { request } from '@/service/request';

export interface Warehouse {
  id: string;
  whCode: string;
  whName: string;
  whType: string;
  enabled: number;
}

export interface StorageLocation {
  id: string;
  whId: string;
  locCode: string;
  locName?: string;
  enabled: number;
}

/** 列出库房。 */
export const getWarehouses = () => {
  return request<Warehouse[]>({ url: '/api/v1/mes/warehouses', method: 'get' });
};

/** 新建库房。 */
export const createWarehouse = (data: { whCode: string; whName: string; whType?: string }) => {
  return request<boolean>({ url: '/api/v1/mes/warehouses', method: 'post', data });
};

/** 列出库位。 */
export const getLocations = () => {
  return request<StorageLocation[]>({ url: '/api/v1/mes/locations', method: 'get' });
};

/** 新建库位。 */
export const createLocation = (data: { whId: string; locCode: string; locName?: string }) => {
  return request<boolean>({ url: '/api/v1/mes/locations', method: 'post', data });
};
