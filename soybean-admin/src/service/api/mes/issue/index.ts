import { request } from '@/service/request';

/** 从源库位领到目标库位。 */
export const issueMaterial = (data: {
  materialLotId: string;
  fromLocationId: string;
  toLocationId: string;
  qty: number;
}) => {
  return request<boolean>({ url: '/api/v1/mes/issues', method: 'post', data });
};

/** 从当前库位退回目标库位。 */
export const returnMaterial = (data: {
  materialLotId: string;
  fromLocationId: string;
  toLocationId: string;
  qty: number;
}) => {
  return request<boolean>({ url: '/api/v1/mes/returns', method: 'post', data });
};
