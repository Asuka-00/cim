import type { CommonStateValue } from '@/service/api/common';

export interface SystemConfigVO {
  id: string;
  tenantId: string;
  createBy: string;
  createAt: string;
  updateBy: string;
  updateAt: string;
  configCode: string;
  configValue: string;
  valueType: string;
  groupCode: string;
  remark: string;
  state: CommonStateValue;
}

export interface SystemConfigQuery {
  configCode?: string;
  configValue?: string;
  valueType?: string;
  groupCode?: string;
  state?: CommonStateValue;
}

export interface SystemConfigCreateRequestDTO {
  configCode: string;
  configValue: string;
  valueType: string;
  groupCode: string;
  remark: string;
  state: CommonStateValue;
}

export interface SystemConfigUpdateRequestDTO {
  configValue: string;
  valueType: string;
  groupCode: string;
  remark: string;
  state: CommonStateValue;
}
