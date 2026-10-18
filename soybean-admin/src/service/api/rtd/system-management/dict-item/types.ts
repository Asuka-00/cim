import type { CommonStateValue } from '@/service/api/common';

export type DictLabels = {
  zh: string;
  en?: string;
};

export interface DictItemVO {
  id: string;
  tenantId: string;
  createBy: string;
  createAt: string;
  updateBy: string;
  updateAt: string;
  dictType: string;
  itemCode: string;
  itemLabel: string;
  labels?: DictLabels;
  sortOrder: number;
  cssClass: string;
  extraJson: string;
  enabled: CommonStateValue;
}

export interface DictItemQuery {
  dictType?: string;
  itemCode?: string;
  itemLabel?: string;
  enabled?: CommonStateValue;
}

export interface DictItemCreateRequestDTO {
  dictType: string;
  itemCode: string;
  itemLabel?: string;
  labels: DictLabels;
  sortOrder?: number;
  cssClass?: string;
  extraJson?: string;
  enabled: CommonStateValue;
}

export interface DictItemUpdateRequestDTO {
  itemLabel?: string;
  labels: DictLabels;
  sortOrder?: number;
  cssClass?: string;
  extraJson?: string;
  enabled: CommonStateValue;
}
