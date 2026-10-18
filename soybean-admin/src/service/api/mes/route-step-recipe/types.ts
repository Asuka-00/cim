/** 步骤 Recipe 覆盖。产品为空表示该步骤通用，会整组替换站点级绑定。 */
export interface RouteStepRecipe {
  id: string;
  routeStepId: string;
  recipeId: string;
  productId: string;
  isDefault: number;
}

/** 步骤 Recipe 覆盖保存请求。 */
export interface RouteStepRecipeSaveRequest {
  routeStepId: string;
  recipeId: string | null;
  productId?: string | null;
  isDefault: number;
}
