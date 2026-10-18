/** 物料单位换算。materialId 为 0 表示不限物料。 */
export interface UomConversion {
  id: string;
  fromUomId: string;
  toUomId: string;
  materialId: string;
  factor: number;
}

/** 新增或修改物料单位换算。 */
export interface UomConversionSaveRequest {
  fromUomId: string;
  toUomId: string;
  materialId?: string | null;
  factor: number;
}
