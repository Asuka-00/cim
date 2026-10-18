import { request } from '@/service/request';

export interface RouteStepSampling {
  id: string;
  routeVersionId: string;
  routeStepId: string;
  productId?: string | number;
  ruleId: string;
}

/** 列出某个工艺版本的步骤抽检覆盖。 */
export const getRouteStepSamplings = (routeVersionId: string) => {
  return request<RouteStepSampling[]>({ url: '/api/v1/mes/route-step-samplings', method: 'get', params: { routeVersionId } });
};

/** 保存覆盖。有记录后，这一步整组替换站点抽检绑定。 */
export const saveRouteStepSampling = (data: {
  routeVersionId: string;
  seqNo: number;
  productId?: string | null;
  ruleId: string;
}) => {
  return request<boolean>({ url: '/api/v1/mes/route-step-samplings', method: 'post', data });
};
