/** 设备台账。当前状态不在保存请求里改。 */
export interface Equipment {
  id: string;
  equipmentCode: string;
  equipmentName: string;
  equipmentType: string;
  groupId?: string | null;
  areaId?: string | null;
  vendor?: string;
  model?: string;
  serialNo?: string;
  e10State: string;
  statusCode?: string;
  reasonCode?: string;
  statusSource: string;
  remark?: string;
  enabled: number;
}

/** 新增或修改设备台账。 */
export interface EquipmentSaveRequest {
  equipmentCode: string;
  equipmentName: string;
  equipmentType?: string;
  groupId?: string | null;
  areaId?: string | null;
  vendor?: string;
  model?: string;
  serialNo?: string;
  remark?: string;
  enabled?: number;
}

/** 手工切换设备状态。 */
export interface EquipmentStatusChangeRequest {
  e10State: string;
  statusCode?: string;
  reasonCode?: string;
  remark?: string;
}

/** 设备状态时间段。 */
export interface EquipmentStatusHist {
  id: string;
  portId?: string;
  fromE10State?: string;
  toE10State: string;
  reasonCode?: string;
  startAt: string;
  endAt?: string | null;
  durationSec?: number | null;
  source: string;
}
