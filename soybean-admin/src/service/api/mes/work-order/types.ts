/** 工单。id 和外键为雪花短整型，接口按 JSON 数字返回。 */
export interface WorkOrder {
  id: string;
  woNo: string;
  productId?: string;
  productVersionId?: string;
  planQty: number;
  qtyUnitId?: string;
  priority?: number;
  dueDate?: string | null;
  status: string;
  lotCount?: number;
  remark?: string;
}

/** 创建工单。Route 和 BOM 版本由服务固化。 */
export interface WorkOrderSaveRequest {
  woNo: string;
  productVersionId: string;
  planQty: number;
  qtyUnitId: string;
  priority?: number;
  dueDate?: string | null;
  remark?: string;
}
