/** 设备端口。Port 沿用设备状态，Chamber 有自己的 E10。 */
export interface EquipmentPort {
  id: string;
  equipmentId: string;
  portCode: string;
  portName?: string;
  kind: string;
  maxConcurrent: number;
  e10State: string;
  enabled: number;
}

/** 端口台账保存请求。不改当前 E10。 */
export interface EquipmentPortSaveRequest {
  portCode: string;
  portName?: string;
  kind: string;
  maxConcurrent: number | null;
  enabled: number;
}
