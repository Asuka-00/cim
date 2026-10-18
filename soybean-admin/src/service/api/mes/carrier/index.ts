import { request } from '@/service/request';

export interface Carrier {
  id: string;
  carrierCode: string;
  carrierType: string;
  capacity?: number;
  status: string;
  currentLotId?: string | null;
  useCount?: number;
  enabled?: number;
}

/** 列出全部载具。 */
export const getCarriers = () => {
  return request<Carrier[]>({ url: '/api/v1/mes/carriers', method: 'get' });
};

/** 新建空载具。类型空则按 FOUP，容量空则 25。 */
export const createCarrier = (data: { carrierCode: string; carrierType?: string; capacity?: number }) => {
  return request<boolean>({ url: '/api/v1/mes/carriers', method: 'post', data });
};

/** 把空载具绑到等待或加工中的 Lot。 */
export const bindCarrier = (carrierId: string, lotId: string) => {
  return request<boolean>({ url: `/api/v1/mes/carriers/${carrierId}/bind/${lotId}`, method: 'post' });
};

/** 解绑。 */
export const unbindCarrier = (carrierId: string) => {
  return request<boolean>({ url: `/api/v1/mes/carriers/${carrierId}/unbind`, method: 'post' });
};
