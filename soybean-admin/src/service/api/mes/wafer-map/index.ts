import { request } from '@/service/request';

export interface WaferMap {
  id?: string;
  waferId: string;
  lotId?: string;
  mapVersion: number;
  isCurrent?: number;
  fileName?: string;
  totalDie?: number;
  goodDie?: number;
  badDie?: number;
}

/** 列出某个 Lot 的全部 Map 版本。 */
export const getWaferMaps = (lotId: string) => {
  return request<WaferMap[]>({ url: `/api/v1/mes/lots/${lotId}/wafer-maps`, method: 'get' });
};

/** 导入一版。返回新版本号。重测会生成下一版。 */
export const importWaferMap = (lotId: string, data: { waferId: string; fileName?: string; csv: string }) => {
  return request<number>({ url: `/api/v1/mes/lots/${lotId}/wafer-maps`, method: 'post', data });
};
