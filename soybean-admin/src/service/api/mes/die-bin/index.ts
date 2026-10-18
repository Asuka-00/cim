import { request } from '@/service/request';

export interface DieBin {
  id?: string;
  lotId?: string;
  waferId?: string | number | null;
  binCode: string;
  binName?: string;
  isGood?: number;
  dieQty: number;
  source?: string;
}

export interface DieBinLine {
  binCode: string;
  binName?: string;
  good: boolean;
  dieQty: number;
}

/** 列出 Lot 的 Bin 行。waferId 为 0 是汇总。 */
export const getDieBins = (lotId: string) => {
  return request<DieBin[]>({ url: `/api/v1/mes/lots/${lotId}/die-bins`, method: 'get' });
};

/** 用这些行替换 Lot 的汇总。良品合计必须等于 Lot 数量。 */
export const replaceDieBins = (lotId: string, lines: DieBinLine[]) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/die-bins`, method: 'put', data: lines });
};
