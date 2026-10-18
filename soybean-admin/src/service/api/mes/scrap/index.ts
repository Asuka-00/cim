import { request } from '@/service/request';

/** 整批报废。有存活片时数量由片数决定；没有片时必须填数量。 */
export const scrapLot = (lotId: string, data: { reasonCode: string; scrapQty?: number | null; remark?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/scrap`, method: 'post', data });
};

/** 报废一片。最后一片报废后 Lot 变为 SCRAPPED。 */
export const scrapWafer = (waferId: string, data: { reasonCode: string; remark?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/wafers/${waferId}/scrap`, method: 'post', data });
};
