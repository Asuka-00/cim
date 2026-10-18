import { request } from '@/service/request';

export interface NotifyRule {
  id: string;
  ruleCode: string;
  ruleName: string;
  eventType: string;
  level?: string;
  titleTemplate?: string;
  contentTemplate?: string;
  receiverType?: string;
  receiverId?: string;
}

/** 按事件类型列出已启用的通知规则。 */
export const getNotifyRules = (eventType: string) => {
  return request<NotifyRule[]>({ url: '/api/v1/mes/notify-rules', method: 'get', params: { eventType } });
};

/** 新建并启用规则。 */
export const createNotifyRule = (data: {
  ruleCode: string;
  ruleName: string;
  eventType: string;
  titleTemplate?: string;
  contentTemplate?: string;
  level?: string;
  receiverType?: string;
  receiverId?: string;
}) => {
  return request<boolean>({ url: '/api/v1/mes/notify-rules', method: 'post', data });
};

/** 停用规则。 */
export const disableNotifyRule = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/notify-rules/${id}/disable`, method: 'post' });
};
