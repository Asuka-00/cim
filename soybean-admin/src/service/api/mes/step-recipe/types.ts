/** 站点 Recipe 绑定。产品为 0 表示通用。 */
export interface StepRecipe {
  id: string;
  stepId: string;
  recipeId: string;
  productId: string;
  isDefault: number;
}

/** 站点 Recipe 绑定保存请求。产品留空按通用。 */
export interface StepRecipeSaveRequest {
  stepId: string;
  recipeId: string | null;
  productId?: string | null;
  isDefault: number;
}
