/** 设备能力。Recipe 或产品为 0 表示不限。 */
export interface EquipmentCapability {
  id: string;
  equipmentId: string;
  stepId: string;
  recipeId: string;
  productId: string;
  status: string;
  validFrom?: string | null;
  validTo?: string | null;
  remark?: string;
}

/** 能力判定。MATCH 和 GROUP 放行。 */
export interface CapabilityDecision {
  allowed: boolean;
  code: string;
}

/** 新增能力。Recipe、产品留空按通用。 */
export interface EquipmentCapabilitySaveRequest {
  equipmentId: string;
  stepId: string | null;
  recipeId?: string | null;
  productId?: string | null;
  status: string;
  validFrom?: string | null;
  validTo?: string | null;
  remark?: string;
}
