import { request } from '@/service/request';

export interface ConfirmRule {
  id: string;
  actionCode: string;
  confirmMode: string;
  secondRoleCode?: string;
  enabled?: number;
}

/** 列出全部二次确认规则。 */
export const getConfirmRules = () => {
  return request<ConfirmRule[]>({ url: '/api/v1/mes/confirm-rules', method: 'get' });
};

/** 查询这个动作当前需要的确认方式。没有启用规则时返回 NONE。 */
export const getConfirmMode = (actionCode: string) => {
  return request<string>({ url: `/api/v1/mes/confirm-rules/${actionCode}`, method: 'get' });
};

/** 新建规则。第二人签名必须填角色。 */
export const createConfirmRule = (data: { actionCode: string; confirmMode: string; secondRoleCode?: string }) => {
  return request<boolean>({ url: '/api/v1/mes/confirm-rules', method: 'post', data });
};

/** 为一次需要确认的动作签名。再次确认由当前用户签。 */
export const signAction = (data: { actionCode: string; bizType: string; bizId: string; signer?: string }) => {
  return request<boolean>({ url: '/api/v1/mes/esigns', method: 'post', data });
};
