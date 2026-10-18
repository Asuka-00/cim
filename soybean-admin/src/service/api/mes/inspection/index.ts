import { request } from '@/service/request';

export interface MaterialLot {
  id: string;
  lotCode: string;
  materialId: string;
  receivedQty?: number;
  uomId?: string;
  status: string;
  qcResult?: string;
  expireDate?: string | null;
  receivedAt?: string | null;
}

/** 列出物料批。 */
export const getMaterialLots = () => {
  return request<MaterialLot[]>({ url: '/api/v1/mes/material-lots', method: 'get' });
};

/** 提交来料检验。PASS 和 CONCESSION 变为可用，FAIL 进入隔离。 */
export const inspectMaterialLot = (materialLotId: string, data: { result: string; remark?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/material-lots/${materialLotId}/inspections`, method: 'post', data });
};
