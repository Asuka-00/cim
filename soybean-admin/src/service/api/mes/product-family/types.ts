/** 产品族。id 为雪花短整型，接口按 JSON 数字返回。 */
export interface ProductFamily {
  id: string;
  familyCode: string;
  familyName: string;
  remark?: string;
  enabled: number;
}

/** 新增或修改产品族。 */
export interface ProductFamilySaveRequest {
  familyCode: string;
  familyName: string;
  remark?: string;
  enabled?: number;
}
