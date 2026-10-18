import { request } from '@/service/request';

export interface RouteStepCollectItem {
  id: string;
  routeVersionId: string;
  routeStepId: string;
  itemId: string;
  productId?: string | number;
  lsl?: number | null;
  usl?: number | null;
  autoHoldOnOos?: number;
  holdReasonCode?: string;
  isRequired?: number;
}

/** 列出某个工艺版本的步骤采集规格。 */
export const getRouteStepCollectItems = (routeVersionId: string) => {
  return request<RouteStepCollectItem[]>({
    url: '/api/v1/mes/route-step-collect-items',
    method: 'get',
    params: { routeVersionId }
  });
};

/** 保存覆盖。有记录后，这一步整组替换站点采集规格。 */
export const saveRouteStepCollectItem = (data: {
  routeVersionId: string;
  seqNo: number;
  itemId: string;
  productId?: string | null;
  lsl?: number | null;
  usl?: number | null;
  autoHoldOnOos?: number;
  holdReasonCode?: string;
  isRequired?: number;
}) => {
  return request<boolean>({ url: '/api/v1/mes/route-step-collect-items', method: 'post', data });
};
