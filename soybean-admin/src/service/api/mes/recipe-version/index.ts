import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';
import type { RecipeVersion, RecipeVersionSaveRequest } from './types';

export type { RecipeVersion, RecipeVersionSaveRequest } from './types';

/** 分页查询某个 Recipe 的版本。 */
export const getRecipeVersionList = (recipeId: string, params: { page: number; pageSize: number }) => {
  return request<PageDTO<RecipeVersion>>({ url: `/api/v1/mes/recipes/${recipeId}/versions`, method: 'get', params });
};

/** 新增草稿。 */
export const createRecipeVersion = (recipeId: string, data: RecipeVersionSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/recipes/${recipeId}/versions`, method: 'post', data });
};

/** 修改草稿。 */
export const updateRecipeVersion = (recipeId: string, id: string, data: RecipeVersionSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/recipes/${recipeId}/versions/${id}`, method: 'put', data });
};

/** 复制出下一个草稿。 */
export const copyRecipeVersion = (recipeId: string, id: string) => {
  return request<boolean>({ url: `/api/v1/mes/recipes/${recipeId}/versions/${id}/copy`, method: 'post' });
};

/** 提交审批。 */
export const submitRecipeVersion = (recipeId: string, id: string, data: { assignee: string; comment?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/recipes/${recipeId}/versions/${id}/submit`, method: 'post', data });
};

/** 审批通过并生效。 */
export const approveRecipeVersion = (recipeId: string, id: string, data: { operator: string; comment?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/recipes/${recipeId}/versions/${id}/approve`, method: 'post', data });
};

/** 驳回为草稿。 */
export const rejectRecipeVersion = (recipeId: string, id: string, data: { operator: string; comment?: string }) => {
  return request<boolean>({ url: `/api/v1/mes/recipes/${recipeId}/versions/${id}/reject`, method: 'post', data });
};

/** 删除草稿。 */
export const deleteRecipeVersion = (recipeId: string, id: string) => {
  return request<boolean>({ url: `/api/v1/mes/recipes/${recipeId}/versions/${id}`, method: 'delete' });
};
