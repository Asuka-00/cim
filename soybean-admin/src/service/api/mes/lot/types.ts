/** Lot。id 和外键为雪花短整型，接口按 JSON 数字返回。 */
export interface Lot {
  id: string;
  lotNo: string;
  lotType?: string;
  woId?: string;
  status: string;
  qty?: number;
  routeSeqNo?: number;
  reworkId?: string | null;
  stepId?: string;
  holdCount?: number;
  priority?: number;
  productId?: string;
  passNo?: number;
  waferCount?: number;
  equipmentId?: string;
  parentLotId?: string;
}

/** 开批。工艺版本从工单带出。 */
export interface LotStartRequest {
  lotNo: string;
  lotType?: string;
  qty: number;
  remark?: string;
}
