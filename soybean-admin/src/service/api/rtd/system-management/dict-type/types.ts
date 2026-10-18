import type { CommonStateValue } from '@/service/api/common';

export interface DictTypeVO {
  id: string;
  tenantId: string;
  createBy: string;
  createAt: string;
  updateBy: string;
  updateAt: string;
  dictType: string;
  dictName: string;
  remark: string;
  enabled: CommonStateValue;
}

export interface DictTypeQuery {
  dictType?: string;
  dictName?: string;
  enabled?: CommonStateValue;
}

export interface DictTypeCreateRequestDTO {
  dictType: string;
  dictName: string;
  remark?: string;
  enabled: CommonStateValue;
}

export interface DictTypeUpdateRequestDTO {
  dictName: string;
  remark?: string;
  enabled: CommonStateValue;
}
