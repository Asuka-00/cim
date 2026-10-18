/** 站点。目检、HOLD、SHIP、仓储默认不选设备和 Recipe。 */
export interface Step {
  id: string;
  stepCode: string;
  stepName: string;
  stepType: string;
  areaId?: string | null;
  stepGroupId?: string | null;
  needEquipment: number;
  needRecipe: number;
  description?: string;
  enabled: number;
}

/** 新增或修改站点。 */
export interface StepSaveRequest {
  stepCode: string;
  stepName: string;
  stepType?: string;
  areaId?: string | null;
  stepGroupId?: string | null;
  needEquipment?: number;
  needRecipe?: number;
  description?: string;
  enabled?: number;
}
