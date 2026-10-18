import { request } from '@/service/request';

export interface Batch {
  id: string;
  batchNo: string;
  stepId?: string;
  status: string;
  lotCount?: number;
  minLots?: number;
  maxLots?: number;
}

/** 列出批次。 */
export const getBatches = () => {
  return request<Batch[]>({ url: '/api/v1/mes/batches', method: 'get' });
};

/** READY 批次的全部成员一起进站。 */
export const trackInBatch = (batchId: string, data: { equipmentId: string; recipeVersionId?: string | null }) => {
  return request<boolean>({ url: `/api/v1/mes/batches/${batchId}/track-in`, method: 'post', data });
};

/** RUNNING 批次的全部成员一起出站。 */
export const trackOutBatch = (batchId: string, data: { remark?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/batches/${batchId}/track-out`, method: 'post', data });
};

export interface BatchMember {
  lotId: string;
  seq?: number;
}

/** 新建空批次。 */
export const createBatch = (data: { batchNo: string; stepId: string; minLots?: number; maxLots: number }) => {
  return request<boolean>({ url: '/api/v1/mes/batches', method: 'post', data });
};

/** 列出仍在批内的成员。 */
export const getBatchMembers = (batchId: string) => {
  return request<BatchMember[]>({ url: `/api/v1/mes/batches/${batchId}/lots`, method: 'get' });
};

/** 加入等待中且未 Hold 的 Lot。 */
export const addBatchLot = (batchId: string, lotId: string) => {
  return request<boolean>({ url: `/api/v1/mes/batches/${batchId}/lots/${lotId}`, method: 'post' });
};

/** 进站前移出 Lot。 */
export const removeBatchLot = (batchId: string, lotId: string) => {
  return request<boolean>({ url: `/api/v1/mes/batches/${batchId}/lots/${lotId}/remove`, method: 'post' });
};

/** 取消尚未进站的批次。 */
export const cancelBatch = (batchId: string) => {
  return request<boolean>({ url: `/api/v1/mes/batches/${batchId}/cancel`, method: 'post' });
};

/** 加工中剔除一个 Lot，该 Lot 回到等待并离开批次。 */
export const ejectBatchLot = (batchId: string, lotId: string, data: { reasonCode: string; remark?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/batches/${batchId}/lots/${lotId}/eject`, method: 'post', data });
};
