/** 设备组。 */
export interface EquipmentGroup {
  id: string;
  groupCode: string;
  groupName: string;
  areaId?: string | null;
  remark?: string;
  enabled: number;
}

/** 新增或修改设备组。 */
export interface EquipmentGroupSaveRequest {
  groupCode: string;
  groupName: string;
  areaId?: string | null;
  remark?: string;
  enabled?: number;
}
