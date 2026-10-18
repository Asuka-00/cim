import type { RouteRecordRaw } from 'vue-router';
import type { CustomRoute } from '@elegant-router/types';
import { layouts, views } from '../elegant/imports';
import { transformElegantRoutesToVueRoutes } from '../elegant/transform';

/** Guests land on login. Signed-in visits are sent to the first menu by the route guard. */
const ROOT_ROUTE: CustomRoute = {
  name: 'root',
  path: '/',
  redirect: '/login',
  meta: {
    title: 'root',
    constant: true
  }
};

const NOT_FOUND_ROUTE: CustomRoute = {
  name: 'not-found',
  path: '/:pathMatch(.*)*',
  component: 'layout.blank$view.404',
  meta: {
    title: 'not-found',
    constant: true
  }
};

/** builtin routes, it must be constant and setup in vue-router */
const builtinRoutes: CustomRoute[] = [ROOT_ROUTE, NOT_FOUND_ROUTE];

/**
 * Pages that are not driven by the backend permission tree.
 *
 * OAuth2 callback is public and reached by a full-page redirect from the identity provider.
 */
function createStandaloneVueRoutes(): RouteRecordRaw[] {
  return [
    {
      path: '/login/oauth2/callback',
      name: 'oauth2-callback-layout',
      component: layouts.blank,
      meta: { title: 'OAuth2', constant: true, hideInMenu: true },
      children: [
        {
          path: '',
          name: 'rtd_oauth2_callback',
          component: () => import('@/views/rtd/oauth2-callback/index.vue'),
          meta: { title: 'OAuth2', constant: true, hideInMenu: true }
        }
      ]
    }
  ];
}

/** create builtin vue routes */
export function createBuiltinVueRoutes() {
  const elegant = transformElegantRoutesToVueRoutes(builtinRoutes, layouts as any, views as any);
  return [...elegant, ...createStandaloneVueRoutes()];
}
