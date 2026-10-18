import type { ProxyOptions } from 'vite';
import { bgRed, bgYellow, green, lightBlue } from 'kolorist';
import { consola } from 'consola';
import { createServiceConfig } from '../../src/utils/service';

/**
 * Paths forwarded to the backend as-is (no prefix rewrite).
 *
 * - `/api`: full-page navigations and SSE / OAuth2 redirects that bypass the axios instance
 * - `/oauth2`, `/login/oauth2`: Spring Security OAuth2 authorization / callback endpoints
 */
const PASSTHROUGH_PATHS = ['/api', '/oauth2', '/login/oauth2'];

/**
 * Set http proxy
 *
 * @param env - The current env
 * @param enable - If enable http proxy
 */
export function createViteProxy(env: Env.ImportMeta, enable: boolean) {
  const isEnableHttpProxy = enable && env.VITE_HTTP_PROXY === 'Y';

  if (!isEnableHttpProxy) return undefined;

  const isEnableProxyLog = env.VITE_PROXY_LOG === 'Y';

  const service = createServiceConfig(env);

  const proxy: Record<string, ProxyOptions> = createProxyItem(service, isEnableProxyLog);

  PASSTHROUGH_PATHS.forEach(path => {
    proxy[path] = { target: service.baseURL, changeOrigin: true };
  });

  return proxy;
}

function createProxyItem(item: App.Service.ServiceConfig, enableLog: boolean) {
  const proxy: Record<string, ProxyOptions> = {};

  proxy[item.proxyPattern] = {
    target: item.baseURL,
    changeOrigin: true,
    configure: (_proxy, options) => {
      _proxy.on('proxyReq', (_proxyReq, req, _res) => {
        if (!enableLog) return;

        const requestUrl = `${lightBlue('[proxy url]')}: ${bgYellow(` ${req.method} `)} ${green(`${item.proxyPattern}${req.url}`)}`;

        const proxyUrl = `${lightBlue('[real request url]')}: ${green(`${options.target}${req.url}`)}`;

        consola.log(`${requestUrl}\n${proxyUrl}`);
      });
      _proxy.on('error', (_err, req, _res) => {
        if (!enableLog) return;
        consola.log(bgRed(`Error: ${req.method} `), green(`${options.target}${req.url}`));
      });
    },
    rewrite: path => path.replace(new RegExp(`^${item.proxyPattern}`), '')
  };

  return proxy;
}
