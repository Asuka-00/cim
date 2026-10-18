import { request } from '@/service/request';
import type { RouteStepRecipe, RouteStepRecipeSaveRequest } from './types';

export type { RouteStepRecipe, RouteStepRecipeSaveRequest } from './types';

/** 列出步骤上已保存的覆盖，不包含站点级回退。 */
export const getOwnedRouteStepRecipes = (routeStepId: string) => {
  return request<RouteStepRecipe[]>({ url: '/api/v1/mes/route-step-recipes/owned', method: 'get', params: { routeStepId } });
};

/** 新增覆盖。只有草稿版本可以改。 */
export const createRouteStepRecipe = (data: RouteStepRecipeSaveRequest) => {
  return request<boolean>({ url: '/api/v1/mes/route-step-recipes', method: 'post', data });
};

/** 修改覆盖。 */
export const updateRouteStepRecipe = (id: string, data: RouteStepRecipeSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/route-step-recipes/${id}`, method: 'put', data });
};

/** 删除覆盖。 */
export const deleteRouteStepRecipe = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/route-step-recipes/${id}`, method: 'delete' });
};
