import { request } from '@/service/request';
import type { StepRecipe, StepRecipeSaveRequest } from './types';

export type { StepRecipe, StepRecipeSaveRequest } from './types';

/** 列出站点上已保存的全部绑定。 */
export const getOwnedStepRecipes = (stepId: string) => {
  return request<StepRecipe[]>({ url: '/api/v1/mes/step-recipes/owned', method: 'get', params: { stepId } });
};

/** 新增绑定。同一站点、同一产品只保留一个默认。 */
export const createStepRecipe = (data: StepRecipeSaveRequest) => {
  return request<boolean>({ url: '/api/v1/mes/step-recipes', method: 'post', data });
};

/** 修改绑定。 */
export const updateStepRecipe = (id: string, data: StepRecipeSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/step-recipes/${id}`, method: 'put', data });
};

/** 删除绑定。 */
export const deleteStepRecipe = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/step-recipes/${id}`, method: 'delete' });
};
