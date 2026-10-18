/** 区域。parentId 为空表示顶层。 */
export interface Area {
  id: string;
  areaCode: string;
  areaName: string;
  parentId?: string | null;
  remark?: string;
  enabled: number;
}

/** 新增或修改区域。 */
export interface AreaSaveRequest {
  areaCode: string;
  areaName: string;
  parentId?: string | null;
  remark?: string;
  enabled?: number;
}
