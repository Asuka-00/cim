import type { PageDTO } from '@/service/api/common';
import { request } from '@/service/request';

export interface DomainEvent {
  id: string;
  eventNo?: string;
  eventType?: string;
  aggregateType?: string;
  aggregateId?: string;
  status?: string;
  retryCount?: number;
  errorMessage?: string;
  payloadJson?: string;
}

export interface EventDelivery {
  id: string;
  eventId?: string;
  subscriber?: string;
  aggregateType?: string;
  status?: string;
  retryCount?: number;
  errorMessage?: string;
}

/** 分页查询领域事件。状态留空表示全部。 */
export const getEventPage = (params: { page: number; pageSize: number; status?: string }) => {
  return request<PageDTO<DomainEvent>>({ url: '/api/v1/mes/events', method: 'get', params });
};

/** 分页查询事件投递。 */
export const getDeliveryPage = (params: { page: number; pageSize: number; subscriber?: string; status?: string }) => {
  return request<PageDTO<EventDelivery>>({ url: '/api/v1/mes/event-deliveries', method: 'get', params });
};

/** 把投递重新置为待处理，并清掉重试次数。 */
export const retryDelivery = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/event-deliveries/${id}/retry`, method: 'post' });
};

/** 跳过投递，不再重试。 */
export const skipDelivery = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/event-deliveries/${id}/skip`, method: 'post' });
};
