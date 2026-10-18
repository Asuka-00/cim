import { request } from '@/service/request';

/** 为指定租户幂等写入编号规则、字典、参数和角色授权。 */
export const initMesTenant = (tenantId: string) => {
  return request<boolean>({ url: `/api/v1/mes/tenants/${tenantId}/init`, method: 'post' });
};
