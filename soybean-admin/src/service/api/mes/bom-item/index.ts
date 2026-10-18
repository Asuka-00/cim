import { request } from '@/service/request';
import type { BomItem, BomItemSaveRequest } from './types';

export type { BomItem, BomItemSaveRequest } from './types';

/** 列出某个版本的 BOM 行，按行号从小到大。 */
export const getBomItemList = (bomVersionId: string) => {
  return request<BomItem[]>({ url: `/api/v1/mes/bom-versions/${bomVersionId}/items`, method: 'get' });
};

/** 新增 BOM 行。只有草稿版本可以改。 */
export const createBomItem = (bomVersionId: string, data: BomItemSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/bom-versions/${bomVersionId}/items`, method: 'post', data });
};

/** 修改 BOM 行。 */
export const updateBomItem = (bomVersionId: string, id: string, data: BomItemSaveRequest) => {
  return request<boolean>({ url: `/api/v1/mes/bom-versions/${bomVersionId}/items/${id}`, method: 'put', data });
};

/** 删除 BOM 行。 */
export const deleteBomItem = (bomVersionId: string, id: string) => {
  return request<boolean>({ url: `/api/v1/mes/bom-versions/${bomVersionId}/items/${id}`, method: 'delete' });
};
