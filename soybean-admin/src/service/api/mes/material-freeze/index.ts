import { request } from '@/service/request';

/** 把可用物料批整批冻结。冻结后不能领料。 */
export const freezeMaterialLot = (materialLotId: string) => {
  return request<boolean>({ url: `/api/v1/mes/material-lots/${materialLotId}/freeze`, method: 'post' });
};

/** 把冻结物料批解冻为可用。 */
export const unfreezeMaterialLot = (materialLotId: string) => {
  return request<boolean>({ url: `/api/v1/mes/material-lots/${materialLotId}/unfreeze`, method: 'post' });
};
