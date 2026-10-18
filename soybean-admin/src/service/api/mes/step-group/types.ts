/** 站点组。 */
export interface StepGroup {
  id: string;
  groupCode: string;
  groupName: string;
  remark?: string;
  enabled: number;
}

/** 站点组保存请求。 */
export interface StepGroupSaveRequest {
  groupCode: string;
  groupName: string;
  remark?: string;
  enabled: number;
}
