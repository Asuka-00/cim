import { request } from '@/service/request';

export interface StepInstruction {
  title: string;
  content: string;
  fileId?: string | null;
}

/** 取出指导。产品留空按通用。产品专用优先于通用。没有时为空。 */
export const getStepInstruction = (params: { stepId: string; productId?: string }) => {
  return request<StepInstruction | null>({ url: '/api/v1/mes/step-instructions', method: 'get', params });
};

/** 保存指导。同一站点与产品只能有一条。 */
export const saveStepInstruction = (data: {
  stepId: string;
  productId?: string | null;
  title: string;
  content: string;
  fileId?: string | null;
}) => {
  return request<boolean>({ url: '/api/v1/mes/step-instructions', method: 'post', data });
};
