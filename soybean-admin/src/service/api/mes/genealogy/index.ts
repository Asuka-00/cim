import { request } from '@/service/request';

export interface LotLink {
  lotId: string;
  lotNo: string;
  qty?: number;
}

/** 列出从这个 Lot 拆出去的子批。 */
export const getLotChildren = (lotId: string) => {
  return request<LotLink[]>({ url: `/api/v1/mes/lots/${lotId}/children`, method: 'get' });
};

/** 找回父 Lot。不是拆出来的时为空。 */
export const getLotParent = (lotId: string) => {
  return request<LotLink | null>({ url: `/api/v1/mes/lots/${lotId}/parent`, method: 'get' });
};

/** 列出来合入这个 Lot 的来源批。 */
export const getMergeSources = (lotId: string) => {
  return request<LotLink[]>({ url: `/api/v1/mes/lots/${lotId}/merge-sources`, method: 'get' });
};
