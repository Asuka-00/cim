/** Dev-server proxy prefix for the backend service */
export const SERVICE_PROXY_PATTERN = '/proxy-default';

/**
 * Create service config by current env
 *
 * @param env The current env
 */
export function createServiceConfig(env: Env.ImportMeta): App.Service.ServiceConfig {
  return {
    baseURL: env.VITE_SERVICE_BASE_URL,
    proxyPattern: SERVICE_PROXY_PATTERN
  };
}

/**
 * Get backend service base url
 *
 * @param env - the current env
 * @param isProxy - if use proxy
 */
export function getServiceBaseURL(env: Env.ImportMeta, isProxy: boolean) {
  const { baseURL, proxyPattern } = createServiceConfig(env);

  return isProxy ? proxyPattern : baseURL;
}
