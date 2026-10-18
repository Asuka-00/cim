import { request } from '@/service/request';

export interface Tool {
  id: string;
  toolCode: string;
  toolName: string;
  typeId?: string;
  status: string;
  currentEquipmentId?: string | null;
  usedCount?: number;
  enabled?: number;
}

/** 列出全部工具。 */
export const getTools = () => {
  return request<Tool[]>({ url: '/api/v1/mes/tools', method: 'get' });
};

/** 新建可用工具。类型编号目前没有列表，按已有类型填写。 */
export const createTool = (data: { toolCode: string; toolName: string; typeId: string }) => {
  return request<boolean>({ url: '/api/v1/mes/tools', method: 'post', data });
};

/** 把可用工具领到设备上，状态变为 IN_USE。 */
export const checkoutTool = (toolId: string, data: { equipmentId: string }) => {
  return request<boolean>({ url: `/api/v1/mes/tools/${toolId}/checkout`, method: 'post', data });
};

/** 归还使用中的工具。 */
export const returnTool = (toolId: string) => {
  return request<boolean>({ url: `/api/v1/mes/tools/${toolId}/return`, method: 'post' });
};
