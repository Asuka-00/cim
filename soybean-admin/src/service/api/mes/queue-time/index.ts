import { request } from '@/service/request';

/** 扫描未 Hold 且等待超时的 Lot。动作不是 WARN 时按 QUEUE_TIME 扣押。不传时间则用服务器当前时间。 */
export const scanQueueTime = (now?: string) => {
  return request<number>({ url: '/api/v1/mes/queue-times/scan', method: 'post', params: now ? { now } : {} });
};
