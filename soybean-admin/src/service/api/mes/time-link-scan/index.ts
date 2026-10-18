import { request } from '@/service/request';

/** 扫描已过截止时间的时间链。HOLD 扣押并记 BREACHED，WARN 只记 WARNED。不传时间则用服务器当前时间。 */
export const scanTimeLink = (now?: string) => {
  return request<number>({ url: '/api/v1/mes/time-link-timers/scan', method: 'post', params: now ? { now } : {} });
};
