import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';

export interface HoldReason {
  id: string;
  reasonCode: string;
  reasonName: string;
  holdType?: string;
  releaseRoleCode?: string;
  remark?: string;
  enabled: number;
}

export interface LotHoldRow {
  id: string;
  lotId: string;
  reasonCode: string;
  holdSource?: string;
  status: string;
  holdRemark?: string;
}

/** 分页查询 Hold 原因。 */
export const getHoldReasonList = (params: { page: number; pageSize: number; reasonCode?: string }) => {
  return request<PageDTO<HoldReason>>({ url: '/api/v1/mes/hold-reasons', method: 'get', params });
};

/** 新增 Hold 原因。类别空按 OTHER。 */
export const createHoldReason = (data: {
  reasonCode: string;
  reasonName: string;
  holdType?: string;
  releaseRoleCode?: string;
  remark?: string;
  enabled?: number;
}) => {
  return request<boolean>({ url: '/api/v1/mes/hold-reasons', method: 'post', data });
};

/** 修改 Hold 原因。 */
export const updateHoldReason = (
  id: string,
  data: {
    reasonCode: string;
    reasonName: string;
    holdType?: string;
    releaseRoleCode?: string;
    remark?: string;
    enabled?: number;
  }
) => {
  return request<boolean>({ url: `/api/v1/mes/hold-reasons/${id}`, method: 'put', data });
};

/** 删除 Hold 原因。已被批次暂停引用时会拒绝。 */
export const deleteHoldReason = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/hold-reasons/${id}`, method: 'delete' });
};

/** 列出 Lot 上尚未释放的 Hold。 */
export const getActiveHolds = (lotId: string) => {
  return request<LotHoldRow[]>({ url: `/api/v1/mes/lots/${lotId}/holds`, method: 'get' });
};

/** 手工 Hold。 */
export const holdLot = (lotId: string, data: { reasonCode: string; remark?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/hold`, method: 'post', data });
};

/** 释放一条 Hold。 */
export const releaseLotHold = (lotId: string, data: { holdId: string; remark?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/release`, method: 'post', data });
};
