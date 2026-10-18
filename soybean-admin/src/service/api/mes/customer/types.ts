/** 客户。id 为雪花短整型，接口按 JSON 数字返回。 */
export interface Customer {
  id: string;
  customerCode: string;
  customerName: string;
  contact?: string;
  phone?: string;
  remark?: string;
  enabled: number;
}

/** 新增或修改客户。 */
export interface CustomerSaveRequest {
  customerCode: string;
  customerName: string;
  contact?: string;
  phone?: string;
  remark?: string;
  enabled?: number;
}
