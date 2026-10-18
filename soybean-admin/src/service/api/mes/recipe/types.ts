/** Recipe 主数据。设备侧程序体不在这里。 */
export interface Recipe {
  id: string;
  recipeCode: string;
  recipeName: string;
  equipmentType?: string | null;
  description?: string;
  enabled: number;
}

/** 新增或修改 Recipe。 */
export interface RecipeSaveRequest {
  recipeCode: string;
  recipeName: string;
  equipmentType?: string | null;
  description?: string;
  enabled?: number;
}
