import type { PermissionVO } from './rtd/system-management/permission';
import { request } from '../request';

function toLoginToken(vo: Api.Auth.LoginVO): Api.Auth.LoginToken {
  return {
    token: vo.accessToken,
    refreshToken: vo.refreshToken,
    accessExpireSeconds: vo.accessExpireSeconds,
    userName: vo.userName,
    displayName: vo.displayName,
    tenantId: vo.tenantId,
    platformAdmin: vo.platformAdmin,
    tenants: vo.tenants
  };
}

async function requestLoginToken(url: string, data: Record<string, unknown>) {
  const res = await request<Api.Auth.LoginVO>({ url, method: 'post', data });

  return { ...res, data: res.data ? toLoginToken(res.data) : null };
}

/** Password login */
export function fetchLogin(userName: string, password: string, tenantId: string) {
  return requestLoginToken('/api/v1/auth/login', { userName, password, tenantId });
}

/** Exchange the refresh token for a new access token */
export function fetchRefreshToken(refreshToken: string) {
  return requestLoginToken('/api/v1/auth/refresh', { refreshToken });
}

/** Exchange the one-time OAuth2 ticket issued by the callback for a session */
export function fetchExchangeOAuth2Ticket(ticket: string) {
  return requestLoginToken('/api/v1/auth/oauth2/exchange', { ticket });
}

export function fetchLogout() {
  return request<boolean>({
    url: '/api/v1/auth/logout',
    method: 'post'
  });
}

export function fetchOAuth2Providers() {
  return request<Api.Auth.OAuth2Provider[]>({
    url: '/api/v1/auth/oauth2/providers',
    method: 'get'
  });
}

/** Full-page redirect target that starts the OAuth2 authorization flow (proxied to the backend). */
export function buildOAuth2AuthorizeUrl(registrationId: string, tenantId: string) {
  const params = new URLSearchParams({ registrationId, tenantId });
  return `/api/v1/auth/oauth2/authorize?${params.toString()}`;
}

/** Public (pre-login) brand configuration of a tenant */
export function fetchPublicBrand(tenantId?: string) {
  return request<Api.Auth.BrandPublic>({
    url: '/api/v1/auth/brand',
    method: 'get',
    params: tenantId ? { tenantId } : undefined
  });
}

export function fetchTenantDropdown() {
  return request<Api.Auth.TenantSelect[]>({
    url: '/api/v1/tenants/dropdown',
    method: 'get'
  });
}

/** Menu and button permissions of the current user */
export function fetchMyPermissions() {
  return request<PermissionVO[]>({
    url: '/api/v1/users/me/permissions',
    method: 'get'
  });
}

/** Switch the current tenant and re-issue tokens */
export function fetchSwitchTenant(tenantId: string) {
  return requestLoginToken('/api/v1/auth/switch-tenant', { tenantId });
}

/** Tenants the current user can switch to */
export function fetchMyTenants() {
  return request<Api.Auth.TenantSelect[]>({
    url: '/api/v1/users/me/tenants',
    method: 'get'
  });
}
