import { request } from '@/service/request';

/** 确认来料收货，生成物料批并入库。 */
export const receiveMaterial = (data: {
  receiptNo: string;
  lotCode: string;
  materialId: string;
  locationId: string;
  uomId: string;
  qty: number;
  needInspect: boolean;
}) => {
  return request<boolean>({ url: '/api/v1/mes/receipts', method: 'post', data });
};
