/** 计数单位。id 为雪花短整型，接口按 JSON 数字返回。 */
export interface CountUnit {
  id: string;
  unitCode: string;
  unitName: string;
  unitType: string;
  isBase: number;
  sortNo: number;
  remark?: string;
  enabled: number;
}

/** 新增或修改计数单位。 */
export interface CountUnitSaveRequest {
  unitCode: string;
  unitName: string;
  unitType: string;
  isBase?: number;
  sortNo?: number;
  remark?: string;
  enabled?: number;
}
