import type { CommonStateValue } from '@/service/api/common';

export interface TenantVO {
  id: string;
  tenantId: string;
  createBy: string;
  createAt: string;
  updateBy: string;
  updateAt: string;
  tenantName: string;
  enabled: CommonStateValue;
}

export interface TenantQuery {
  tenantId?: string;
  tenantName?: string;
  enabled?: CommonStateValue;
}

export interface TenantCreateRequestDTO {
  tenantId: string;
  tenantName: string;
  enabled: CommonStateValue;
}

export interface TenantUpdateRequestDTO {
  tenantName: string;
  enabled: CommonStateValue;
}

export interface TenantSelectVO {
  id: string;
  tenantId: string;
  tenantName: string;
}
