/** Recipe 版本。ACTIVE 不能改内容。 */
export interface RecipeVersion {
  id: string;
  recipeId: string;
  versionNo: number;
  status: string;
  paramDesc?: string;
  effectiveAt?: string | null;
  expireAt?: string | null;
  remark?: string;
}

/** 草稿的参数说明和备注。 */
export interface RecipeVersionSaveRequest {
  paramDesc?: string;
  remark?: string;
}
