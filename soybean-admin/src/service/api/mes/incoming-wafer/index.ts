import { request } from '@/service/request';

export interface IncomingWafer {
  id: string;
  materialLotId: string;
  vendorWaferId: string;
  slotNo?: number | null;
  status: string;
}

/** 列出物料批上的来料片。 */
export const getIncomingWafers = (materialLotId: string) => {
  return request<IncomingWafer[]>({ url: `/api/v1/mes/material-lots/${materialLotId}/incoming-wafers`, method: 'get' });
};

/** 录入来料片。同一批内编号不能重复。 */
export const recordIncomingWafers = (
  materialLotId: string,
  lines: Array<{ vendorWaferId: string; slotNo?: number | null }>
) => {
  return request<boolean>({ url: `/api/v1/mes/material-lots/${materialLotId}/incoming-wafers`, method: 'post', data: lines });
};
