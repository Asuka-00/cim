import { useAuthStore } from '@/store/modules/auth';
import { localStg } from '@/utils/storage';
import { persistLoginToken } from '@/utils/auth-token';
import { fetchRefreshToken } from '../api';
import type { RequestInstanceState } from './type';

export { persistLoginToken, isAccessTokenNearExpiry } from '@/utils/auth-token';

export function getAuthorization() {
  const token = localStg.get('token');
  return token ? `Bearer ${token}` : null;
}

/** 32-char hex trace id forwarded as `X-Trace-ID`; falls back gracefully outside secure contexts. */
export function createTraceId(): string {
  const c = globalThis.crypto;
  if (typeof c?.randomUUID === 'function') {
    return c.randomUUID().replace(/-/g, '');
  }
  if (typeof c?.getRandomValues === 'function') {
    const bytes = new Uint8Array(16);
    c.getRandomValues(bytes);
    return Array.from(bytes, b => b.toString(16).padStart(2, '0')).join('');
  }
  return `${Date.now().toString(16)}${Math.random().toString(16).slice(2)}`
    .replace(/\./g, '')
    .padEnd(32, '0')
    .slice(0, 32);
}

async function handleRefreshToken() {
  const { resetStore } = useAuthStore();

  const rToken = localStg.get('refreshToken') || '';
  const { error, data } = await fetchRefreshToken(rToken);
  if (!error && data) {
    persistLoginToken(data);
    return true;
  }

  resetStore();

  return false;
}

export async function handleExpiredRequest(state: RequestInstanceState) {
  if (!state.refreshTokenPromise) {
    state.refreshTokenPromise = handleRefreshToken();
  }

  const success = await state.refreshTokenPromise;

  setTimeout(() => {
    state.refreshTokenPromise = null;
  }, 1000);

  return success;
}

export function showErrorMsg(state: RequestInstanceState, message: string) {
  if (!state.errMsgStack?.length) {
    state.errMsgStack = [];
  }

  const isExist = state.errMsgStack.includes(message);

  if (!isExist) {
    state.errMsgStack.push(message);

    window.$message?.error(message, {
      onLeave: () => {
        state.errMsgStack = state.errMsgStack.filter(msg => msg !== message);

        setTimeout(() => {
          state.errMsgStack = [];
        }, 5000);
      }
    });
  }
}
