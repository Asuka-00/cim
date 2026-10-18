import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';

export interface WaferRow {
  id: string;
  waferCode: string;
  lotId?: string;
  slotNo?: number;
  status?: string;
  purpose?: string;
  vendorWaferId?: string;
  scrapReason?: string;
  customerId?: string;
  recycledFromWaferId?: string;
  dummyUseCount?: number;
  scrappedStepId?: string;
  scrappedAt?: string;
  remark?: string;
}

/** 分页查询晶圆。片号和批次都可以留空。 */
export const getWaferPage = (params: { page: number; pageSize: number; waferCode?: string; lotId?: string }) => {
  return request<PageDTO<WaferRow>>({ url: '/api/v1/mes/wafers', method: 'get', params });
};

/** 按主键查询一片。不存在会报错。 */
export const getWafer = (id: string) => {
  return request<WaferRow>({ url: `/api/v1/mes/wafers/${id}`, method: 'get' });
};
