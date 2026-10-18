/** 编号规则。id 为雪花短整型，接口按 JSON 数字返回。 */
export interface SequenceRule {
  id: string;
  ruleCode: string;
  ruleName: string;
  bizType: string;
  prefix?: string;
  datePattern?: string;
  seqLength: number;
  seqStart: number;
  seqStep: number;
  resetCycle: string;
  suffix?: string;
  remark?: string;
  enabled: number;
}

/** 新增或修改编号规则。 */
export interface SequenceRuleSaveRequest {
  ruleCode: string;
  ruleName: string;
  bizType: string;
  prefix?: string;
  datePattern?: string;
  seqLength: number;
  seqStart: number;
  seqStep: number;
  resetCycle: string;
  suffix?: string;
  remark?: string;
  enabled?: number;
}
