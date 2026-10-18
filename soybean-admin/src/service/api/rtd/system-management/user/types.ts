import type { BaseVO, CommonStateValue } from '@/service/api/common';

export interface UserTenantMembership {
  tenantId: string;
  tenantName?: string;
  roleIds: string[];
}

export interface UserVO extends BaseVO {
  userName: string;
  displayName: string;
  enabled: CommonStateValue;
  platformAdmin?: boolean;
  tenantNames?: string;
}

export interface UserDetailVO extends UserVO {
  memberships?: UserTenantMembership[];
}

export interface UserQuery {
  userName?: string;
  displayName?: string;
  enabled?: CommonStateValue;
}

export interface UserCreateRequestDTO {
  userName: string;
  displayName: string;
  password: string;
  enabled: CommonStateValue;
  platformAdmin?: boolean;
  memberships: UserTenantMembership[];
}

export interface UserUpdateRequestDTO {
  displayName: string;
  enabled: CommonStateValue;
  platformAdmin?: boolean;
  memberships: UserTenantMembership[];
}

export interface ChangePasswordRequestDTO {
  oldPassword: string;
  newPassword: string;
}
