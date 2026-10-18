import { request } from '@/service/request';

export interface PackContainer {
  id: string;
  containerNo: string;
  containerType: string;
  lotId?: string;
  qty?: number;
  capacity?: number;
  status: string;
}

/** 列出某个 Lot 的包装容器。 */
export const getPackContainers = (lotId: string) => {
  return request<PackContainer[]>({ url: `/api/v1/mes/lots/${lotId}/containers`, method: 'get' });
};

/** 给在制 Lot 建一个空容器。 */
export const createPackContainer = (lotId: string, data: { containerNo: string; containerType: string; capacity: number }) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/containers`, method: 'post', data });
};

/** 装入。达到容量时容器变为 FULL。 */
export const packContainer = (containerId: string, data: { qty: number }) => {
  return request<boolean>({ url: `/api/v1/mes/containers/${containerId}/pack`, method: 'post', data });
};
