import { localStg } from '@/utils/storage';

const REFRESH_SKEW_MS = 10_000;

export function persistLoginToken(data: { token: string; refreshToken: string; accessExpireSeconds?: number }) {
  localStg.set('token', data.token);
  localStg.set('refreshToken', data.refreshToken);
  if (data.accessExpireSeconds && data.accessExpireSeconds > 0) {
    localStg.set('tokenExpireAt', Date.now() + data.accessExpireSeconds * 1000);
  }
}

export function isAccessTokenNearExpiry() {
  const expireAt = localStg.get('tokenExpireAt');
  return Boolean(expireAt && Date.now() >= expireAt - REFRESH_SKEW_MS);
}
