import { request } from '@/service/request';

/** 把到期日早于指定日期的可用或待检批改为 EXPIRED。返回改了几批。 */
export const expireMaterialLots = (today?: string) => {
  return request<number>({
    url: '/api/v1/mes/material-lots/expire-due',
    method: 'post',
    params: today ? { today } : undefined
  });
};
