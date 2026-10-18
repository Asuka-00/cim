import { request } from '@/service/request';

/** 读出已写入的自定义属性。 */
export const getLotUda = (lotId: string) => {
  return request<Record<string, string>>({ url: `/api/v1/mes/lots/${lotId}/uda`, method: 'get' });
};

/** 查出还没写的必填属性编码。 */
export const getMissingLotUda = (lotId: string) => {
  return request<string[]>({ url: `/api/v1/mes/lots/${lotId}/uda/missing`, method: 'get' });
};

/** 写入一个已定义的属性值。 */
export const putLotUda = (lotId: string, data: { attrCode: string; value: string }) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/uda`, method: 'put', data });
};
