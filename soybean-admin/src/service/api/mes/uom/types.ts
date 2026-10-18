/** 物料计量单位。id 为雪花短整型，接口按 JSON 数字返回。 */
export interface Uom {
  id: string;
  uomCode: string;
  uomName: string;
  dimension: string;
  decimals: number;
  enabled: number;
}

/** 新增或修改物料计量单位。 */
export interface UomSaveRequest {
  uomCode: string;
  uomName: string;
  dimension: string;
  decimals?: number;
  enabled?: number;
}
