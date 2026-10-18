import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';
import type { Recipe, RecipeSaveRequest } from './types';

export type { Recipe, RecipeSaveRequest } from './types';

/** 分页查询 Recipe。 */
export const getRecipeList = (params: { page: number; pageSize: number; recipeCode?: string }) => {
  return request<PageDTO<Recipe>>({ url: '/api/v1/mes/recipes', method: 'get', params });
};

/** 新增 Recipe。 */
export const createRecipe = (data: RecipeSaveRequest) => {
  return request<boolean>({ url: '/api/v1/mes/recipes', method: 'post', data });
};

/** 修改 Recipe。 */
export const updateRecipe = (id: string, data: RecipeSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/recipes/${id}`, method: 'put', data });
};

/** 删除 Recipe。已有版本时后端拒绝。 */
export const deleteRecipe = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/recipes/${id}`, method: 'delete' });
};
