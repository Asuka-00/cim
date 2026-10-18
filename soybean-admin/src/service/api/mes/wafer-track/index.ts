import { request } from '@/service/request';

/** 让选中的片跟原 Lot 进站。没选中的片留在原步骤并拆成子 Lot。 */
export const waferTrackIn = (
  lotId: string,
  data: { waferIds: string[]; stepId: string; equipmentId?: string | null; recipeVersionId?: string | null }
) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/wafer-track/in`, method: 'post', data });
};

/** 让选中的片跟原 Lot 出站。没选中的片留在原步骤并拆成子 Lot。 */
export const waferTrackOut = (lotId: string, data: { waferIds: string[] }) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/wafer-track/out`, method: 'post', data });
};
