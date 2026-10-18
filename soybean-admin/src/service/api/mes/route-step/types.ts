/** Route 步骤。同一站点可以出现多次。 */
export interface RouteStep {
  id: string;
  routeVersionId: string;
  seqNo: number;
  stepId: string;
  stepAlias?: string;
  isRequired: number;
  equipmentGroupId?: string | null;
  countUnitId?: string | null;
  isUnitConvert: number;
  convertToUnitId?: string | null;
  queueTimeMaxMin?: number | null;
  queueTimeWarnMin?: number | null;
  queueTimeAction?: string | null;
}

/** Route 步骤保存请求。转换站点必须带转换后的计数单位。 */
export interface RouteStepSaveRequest {
  seqNo: number | null;
  stepId: string | null;
  stepAlias?: string;
  isRequired: number;
  equipmentGroupId?: string | null;
  countUnitId?: string | null;
  isUnitConvert: number;
  convertToUnitId?: string | null;
  queueTimeMaxMin?: number | null;
  queueTimeWarnMin?: number | null;
  queueTimeAction?: string | null;
}
