import { request } from '@/service/request';

/** 已完成且未 Hold 的 Lot 整批出货。 */
export const shipLot = (lotId: string, data: { shipNo: string; remark?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/ship`, method: 'post', data });
};
