/** 站点与设备组的绑定。优先级数小优先。 */
export interface StepEquipmentGroup {
  id: string;
  stepId: string;
  groupId: string;
  priority: number;
}

/** 站点设备组绑定保存请求。 */
export interface StepEquipmentGroupSaveRequest {
  stepId: string;
  groupId: string | null;
  priority: number | null;
}
