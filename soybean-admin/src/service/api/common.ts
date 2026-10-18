export type PageDTO<T> = Api.Common.PageDTO<T>;
export type BaseVO = Api.Common.BaseVO;
export type CommonStateValue = Api.Common.CommonState;
export type AdminFlagValue = Api.Common.AdminFlag;
export type PermissionTypeValue = Api.Common.PermissionType;
export type VisibilityStateValue = Api.Common.VisibilityState;

export const CommonState = {
  ENABLED: 'ENABLED',
  DISABLED: 'DISABLED'
} as const;

export const AdminFlag = {
  NOT_ADMIN: 'NOT_ADMIN',
  IS_ADMIN: 'IS_ADMIN',
  SUPER_ADMIN: 'SUPER_ADMIN'
} as const;

export const PermissionType = {
  MENU: 'MENU',
  BUTTON: 'BUTTON',
  API: 'API'
} as const;

export const VisibilityState = {
  HIDDEN: 'HIDDEN',
  VISIBLE: 'VISIBLE'
} as const;
