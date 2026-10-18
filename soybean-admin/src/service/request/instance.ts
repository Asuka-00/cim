import type { AxiosResponse } from 'axios';
import { BACKEND_ERROR_CODE, createFlatRequest } from '@sa/axios';
import { useAuthStore } from '@/store/modules/auth';
import { getServiceBaseURL } from '@/utils/service';
import { $t } from '@/locales';
import { createTraceId, getAuthorization, handleExpiredRequest, isAccessTokenNearExpiry, showErrorMsg } from './shared';
import type { RequestInstanceState } from './type';

const isHttpProxy = import.meta.env.DEV && import.meta.env.VITE_HTTP_PROXY === 'Y';
const baseURL = getServiceBaseURL(import.meta.env, isHttpProxy);

/** Endpoints that must be called without a bearer token (and never trigger a refresh). */
const PUBLIC_URL_PATTERNS = ['/auth/refresh', '/auth/login', '/auth/brand', '/tenants/dropdown', '/oauth2'];

function isPublicUrl(url = '') {
  return PUBLIC_URL_PATTERNS.some(pattern => url.includes(pattern));
}

function splitCodes(value?: string) {
  return value?.split(',').filter(Boolean) ?? [];
}

const LOGOUT_CODES = splitCodes(import.meta.env.VITE_SERVICE_LOGOUT_CODES);
const MODAL_LOGOUT_CODES = splitCodes(import.meta.env.VITE_SERVICE_MODAL_LOGOUT_CODES);
const EXPIRED_TOKEN_CODES = splitCodes(import.meta.env.VITE_SERVICE_EXPIRED_TOKEN_CODES);

export const request = createFlatRequest<App.Service.Response, unknown, RequestInstanceState>(
  {
    baseURL,
    timeout: 30000,
    validateStatus: status => status < 500
  },
  {
    defaultState: {
      errMsgStack: [],
      refreshTokenPromise: null
    },
    transform(response) {
      return response.data.data;
    },
    async onRequest(config) {
      const isPublic = isPublicUrl(config.url);

      if (!isPublic && isAccessTokenNearExpiry()) {
        await handleExpiredRequest(request.state);
      }

      config.headers.set('X-Trace-ID', createTraceId());

      if (!isPublic) {
        const authorization = getAuthorization();
        if (authorization) {
          config.headers.set('Authorization', authorization);
        }
      }

      return config;
    },
    isBackendSuccess(response) {
      return String(response.data.code) === import.meta.env.VITE_SERVICE_SUCCESS_CODE;
    },
    async onBackendFail(response, instance) {
      const responseCode = String(response.data.code);
      const message = response.data.message || '';

      function logout() {
        useAuthStore().resetStore();
      }

      function logoutAndCleanup() {
        logout();
        window.removeEventListener('beforeunload', logout);
        request.state.errMsgStack = request.state.errMsgStack.filter(item => item !== message);
      }

      if (LOGOUT_CODES.includes(responseCode)) {
        logout();
        return null;
      }

      if (MODAL_LOGOUT_CODES.includes(responseCode) && !request.state.errMsgStack.includes(message)) {
        request.state.errMsgStack = [...request.state.errMsgStack, message];

        window.addEventListener('beforeunload', logout);

        window.$dialog?.error({
          title: $t('common.error'),
          content: message || $t('common.error'),
          positiveText: $t('common.confirm'),
          maskClosable: false,
          closeOnEsc: false,
          onPositiveClick: logoutAndCleanup,
          onClose: logoutAndCleanup
        });

        return null;
      }

      if (EXPIRED_TOKEN_CODES.includes(responseCode)) {
        const refreshed = await handleExpiredRequest(request.state);
        if (refreshed) {
          response.config.headers.set('Authorization', getAuthorization());

          return instance.request(response.config) as Promise<AxiosResponse>;
        }
      }

      return null;
    },
    onError(error) {
      let message = error.message;
      let backendErrorCode = '';

      if (error.code === BACKEND_ERROR_CODE) {
        message = error.response?.data?.message || message;
        backendErrorCode = String(error.response?.data?.code ?? '');
      }

      if (error.response?.status === 401) {
        if (isPublicUrl(error.config?.url) || EXPIRED_TOKEN_CODES.includes(backendErrorCode)) {
          return;
        }
        useAuthStore().resetStore();
        return;
      }

      // already handled by onBackendFail (dialog / refresh)
      if (MODAL_LOGOUT_CODES.includes(backendErrorCode) || EXPIRED_TOKEN_CODES.includes(backendErrorCode)) {
        return;
      }

      showErrorMsg(request.state, message);
    }
  }
);
