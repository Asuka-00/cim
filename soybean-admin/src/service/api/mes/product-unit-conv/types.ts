/** 产品计数换算。1 个源单位等于 factor 个目标单位。 */
export interface ProductUnitConv {
  id: string;
  productId: string;
  fromUnitId: string;
  toUnitId: string;
  factor: number;
  tolerancePct: number;
  remark?: string;
}

/** 新增或修改产品计数换算。 */
export interface ProductUnitConvSaveRequest {
  fromUnitId: string;
  toUnitId: string;
  factor: number;
  tolerancePct?: number | null;
  remark?: string;
}

/** 按系数核对实际数量后的结果。 */
export interface ProductUnitConvCheck {
  referenceQty: number;
  deviationPct: number;
  withinTolerance: boolean;
}
