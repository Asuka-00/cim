import { computed, nextTick, ref, shallowRef } from 'vue';
import type { RouteRecordRaw } from 'vue-router';
import { defineStore } from 'pinia';
import { useBoolean } from '@sa/hooks';
import type { ElegantConstRoute, LastLevelRouteKey, RouteKey } from '@elegant-router/types';
import { router } from '@/router';
import { fetchMyPermissions } from '@/service/api';
import { SetupStoreId } from '@/enum';
import { createConstantRoutes, getAuthVueRoutes } from '@/router/routes';
import { useAuthStore } from '../auth';
import { useTabStore } from '../tab';
import { buildPermissionRoutes } from './permission';
import {
  filterRoutesByDev,
  getBreadcrumbsByRoute,
  getCacheRouteNames,
  getGlobalMenusByAuthRoutes,
  getSelectedMenuKeyPathByKey,
  sortRoutesByOrder,
  transformMenuToSearchMenus,
  updateLocaleOfGlobalMenus
} from './shared';

export const useRouteStore = defineStore(SetupStoreId.Route, () => {
  const tabStore = useTabStore();
  const { bool: isInitConstantRoute, setBool: setIsInitConstantRoute } = useBoolean();
  const { bool: isInitAuthRoute, setBool: setIsInitAuthRoute } = useBoolean();

  /** Home route key */
  const routeHome = ref(import.meta.env.VITE_ROUTE_HOME);

  /** Home route path. Permission routes replace this with the first menu leaf. */
  const homePath = ref('/403');

  /** In-flight auth-route init; login and the guard can both trigger it */
  let initAuthRouteTask: Promise<void> | null = null;

  /**
   * Set route home
   *
   * @param routeKey Route key
   */
  function setRouteHome(routeKey: LastLevelRouteKey) {
    routeHome.value = routeKey;
  }

  /** constant routes */
  const constantRoutes = shallowRef<ElegantConstRoute[]>([]);

  function addConstantRoutes(routes: ElegantConstRoute[]) {
    const constantRoutesMap = new Map<string, ElegantConstRoute>([]);

    routes.forEach(route => {
      constantRoutesMap.set(route.name, route);
    });

    constantRoutes.value = Array.from(constantRoutesMap.values());
  }

  /** auth routes */
  const authRoutes = shallowRef<ElegantConstRoute[]>([]);

  function addAuthRoutes(routes: ElegantConstRoute[]) {
    const authRoutesMap = new Map<string, ElegantConstRoute>([]);

    routes.forEach(route => {
      authRoutesMap.set(route.name, route);
    });

    authRoutes.value = Array.from(authRoutesMap.values());
  }

  const removeRouteFns: (() => void)[] = [];

  /** Global menus */
  const menus = ref<App.Global.Menu[]>([]);
  const searchMenus = computed(() => transformMenuToSearchMenus(menus.value));

  /** Get global menus */
  function getGlobalMenus(routes: ElegantConstRoute[]) {
    menus.value = getGlobalMenusByAuthRoutes(routes);
  }

  /** Update global menus by locale */
  function updateGlobalMenusByLocale() {
    menus.value = updateLocaleOfGlobalMenus(menus.value);
  }

  /** Cache routes */
  const cacheRoutes = ref<RouteKey[]>([]);

  /**
   * Exclude cache routes
   *
   * for reset route cache
   */
  const excludeCacheRoutes = ref<RouteKey[]>([]);

  /**
   * Get cache routes
   *
   * @param routes Vue routes
   */
  function getCacheRoutes(routes: RouteRecordRaw[]) {
    cacheRoutes.value = getCacheRouteNames(routes);
  }

  /**
   * Reset route cache
   *
   * @default router.currentRoute.value.name current route name
   * @param routeKey
   */
  async function resetRouteCache(routeKey?: RouteKey) {
    const routeName = routeKey || (router.currentRoute.value.name as RouteKey);

    excludeCacheRoutes.value.push(routeName);

    await nextTick();

    excludeCacheRoutes.value = [];
  }

  /** Global breadcrumbs */
  const breadcrumbs = computed(() => getBreadcrumbsByRoute(router.currentRoute.value, menus.value));

  /** Reset store */
  async function resetStore() {
    const routeStore = useRouteStore();

    initAuthRouteTask = null;
    setIsInitAuthRoute(false);
    setIsInitConstantRoute(false);

    routeStore.$reset();

    resetVueRoutes();

    // after reset store, need to re-init constant route
    await initConstantRoute();
  }

  /** Reset vue routes */
  function resetVueRoutes() {
    removeRouteFns.forEach(fn => fn());
    removeRouteFns.length = 0;
  }

  /** init constant route */
  async function initConstantRoute() {
    if (isInitConstantRoute.value) return;

    addConstantRoutes(createConstantRoutes());

    handleConstantAndAuthRoutes();

    setIsInitConstantRoute(true);

    tabStore.initHomeTab();
  }

  /** Init auth route from the backend permission tree */
  async function initAuthRoute() {
    if (isInitAuthRoute.value) return;
    if (initAuthRouteTask) return initAuthRouteTask;

    initAuthRouteTask = doInitAuthRoute().finally(() => {
      initAuthRouteTask = null;
    });

    return initAuthRouteTask;
  }

  async function doInitAuthRoute() {
    const authStore = useAuthStore();

    if (!authStore.userInfo.userId) {
      await authStore.initUserInfo();
    }

    const { data: permissions, error } = await fetchMyPermissions();

    if (error || !permissions) {
      authStore.resetStore();
      return;
    }

    const { menuRoutes, vueRoutes, home, homePath: nextHomePath } = buildPermissionRoutes(permissions);

    addAuthRoutes(menuRoutes);

    const constantVueRoutes = getAuthVueRoutes(filterRoutesByDev(constantRoutes.value));

    resetVueRoutes();

    // drop the placeholder `root` so the permission bundle's redirect is the only `/`
    if (router.hasRoute('root')) {
      router.removeRoute('root');
    }

    addRoutesToVueRouter(constantVueRoutes.filter(route => route.name !== 'root' && route.name !== 'not-found'));
    addRoutesToVueRouter(vueRoutes);

    getGlobalMenus(sortRoutesByOrder(menuRoutes));
    getCacheRoutes(vueRoutes);

    setRouteHome(home as LastLevelRouteKey);
    homePath.value = nextHomePath;

    setIsInitAuthRoute(true);

    tabStore.initHomeTab();
  }

  /** Rebuild auth routes (e.g. after a permission change) and leave pages that no longer exist */
  async function reloadAuthRoutes() {
    const currentPath = router.currentRoute.value.path;

    setIsInitAuthRoute(false);
    await initAuthRoute();

    const stillExists = router.getRoutes().some(route => route.path === currentPath);
    if (!stillExists && currentPath !== '/403') {
      await router.replace('/');
    }
  }

  /** handle constant and auth routes */
  function handleConstantAndAuthRoutes() {
    const allRoutes = filterRoutesByDev([...constantRoutes.value, ...authRoutes.value]);

    const sortRoutes = sortRoutesByOrder(allRoutes);

    const vueRoutes = getAuthVueRoutes(sortRoutes);

    resetVueRoutes();

    addRoutesToVueRouter(vueRoutes);

    getGlobalMenus(sortRoutes);

    getCacheRoutes(vueRoutes);
  }

  /**
   * Add routes to vue router
   *
   * @param routes Vue routes
   */
  function addRoutesToVueRouter(routes: RouteRecordRaw[]) {
    routes.forEach(route => {
      const removeFn = router.addRoute(route);
      addRemoveRouteFn(removeFn);
    });
  }

  /**
   * Add remove route fn
   *
   * @param fn
   */
  function addRemoveRouteFn(fn: () => void) {
    removeRouteFns.push(fn);
  }

  /**
   * Get selected menu key path
   *
   * @param selectedKey Selected menu key
   */
  function getSelectedMenuKeyPath(selectedKey: string) {
    return getSelectedMenuKeyPathByKey(selectedKey, menus.value);
  }

  async function onRouteSwitchWhenLoggedIn() {
    // some global init logic when logged in and switch route
  }

  async function onRouteSwitchWhenNotLoggedIn() {
    // some global init logic if it does not need to be logged in
  }

  return {
    resetStore,
    routeHome,
    homePath,
    menus,
    searchMenus,
    updateGlobalMenusByLocale,
    cacheRoutes,
    excludeCacheRoutes,
    resetRouteCache,
    breadcrumbs,
    initConstantRoute,
    isInitConstantRoute,
    initAuthRoute,
    reloadAuthRoutes,
    isInitAuthRoute,
    setIsInitAuthRoute,
    getSelectedMenuKeyPath,
    onRouteSwitchWhenLoggedIn,
    onRouteSwitchWhenNotLoggedIn
  };
});
