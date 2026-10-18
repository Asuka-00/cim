import { request } from '@/service/request';

export interface UserScope {
  id: string;
  userName: string;
  scopeType: string;
  scopeId: string;
}

/** 列出全部人员范围。 */
export const getUserScopes = () => {
  return request<UserScope[]>({ url: '/api/v1/mes/user-scopes', method: 'get' });
};

/** 授予一项范围。某类型有记录后，该用户只能操作名单里的对象。 */
export const createUserScope = (data: { userName: string; scopeType: string; scopeId: string }) => {
  return request<boolean>({ url: '/api/v1/mes/user-scopes', method: 'post', data });
};

/** 判断该用户能否操作这个对象。该类型还没有记录时为允许。 */
export const allowedUserScope = (params: { userName: string; scopeType: string; scopeId: string }) => {
  return request<boolean>({ url: '/api/v1/mes/user-scopes/allowed', method: 'get', params });
};
