import { request } from '@/service/request';

export interface LotPrint {
  tplId: string;
  copies: number;
  reprint: number;
  printedAt?: string;
}

/** 列出这个 Lot 的打印记录。 */
export const getLotPrints = (lotId: string) => {
  return request<LotPrint[]>({ url: `/api/v1/mes/lots/${lotId}/prints`, method: 'get' });
};

/** 用已有模板记一条打印。份数空则 1。 */
export const recordLotPrint = (lotId: string, data: { tplCode: string; copies?: number }) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/prints`, method: 'post', data });
};
