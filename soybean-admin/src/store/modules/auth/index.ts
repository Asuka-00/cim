import { computed, nextTick, reactive, ref } from 'vue';
import { defineStore } from 'pinia';
import { useLoading } from '@sa/hooks';
import {
  fetchExchangeOAuth2Ticket,
  fetchLogin,
  fetchLogout,
  fetchMyPermissions,
  fetchMyTenants,
  fetchSwitchTenant
} from '@/service/api';
import { useRouterPush } from '@/hooks/common/router';
import { localStg } from '@/utils/storage';
import { persistLoginToken } from '@/utils/auth-token';
import { SetupStoreId } from '@/enum';
import { $t } from '@/locales';
import { router } from '@/router';
import { useRouteStore } from '../route';
import { extractButtonPermissionCodes } from '../route/permission';
import { useTabStore } from '../tab';
import { useBrandStore } from '../brand';
import { pauseSessionEventsForTenantSwitch, resumeSessionEvents } from '@/hooks/business/sse-session';
import { clearAuthStorage, getToken } from './shared';

export const useAuthStore = defineStore(SetupStoreId.Auth, () => {
  const authStore = useAuthStore();
  const tabStore = useTabStore();
  const brandStore = useBrandStore();
  const { toLogin } = useRouterPush(false);
  const { loading: loginLoading, startLoading, endLoading } = useLoading();

  const token = ref('');
  const tenants = ref<Api.Auth.TenantSelect[]>([]);
  const currentTenantId = ref(localStg.get('loginTenantId') || '');
  const platformAdmin = ref(Boolean(localStg.get('platformAdmin')));

  const userInfo: Api.Auth.UserInfo = reactive({
    userId: '',
    userName: '',
    displayName: '',
    roles: [],
    buttons: []
  });

  const isLogin = computed(() => Boolean(token.value));

  async function resetStore() {
    recordUserId();

    try {
      if (getToken()) {
        await fetchLogout();
      }
    } catch {
      // logout is best-effort; local state is cleared regardless
    }

    clearAuthStorage();

    authStore.$reset();

    if (!router.currentRoute.value.meta.constant) {
      await toLogin();
    }

    tabStore.cacheTabs();
    useRouteStore().resetStore();
  }

  function recordUserId() {
    if (!userInfo.userId) {
      return;
    }

    localStg.set('lastLoginUserId', userInfo.userId);
  }

  function checkTabClear(): boolean {
    if (!userInfo.userId) {
      return false;
    }

    const lastLoginUserId = localStg.get('lastLoginUserId');

    if (!lastLoginUserId || lastLoginUserId !== userInfo.userId) {
      localStg.remove('globalTabs');
      tabStore.clearTabs();

      localStg.remove('lastLoginUserId');
      return true;
    }

    localStg.remove('lastLoginUserId');
    return false;
  }

  async function login(userName: string, password: string, tenantId: string, redirect = true) {
    startLoading();

    const { data: loginToken, error } = await fetchLogin(userName, password, tenantId);

    if (!error && loginToken) {
      persistTenantContext(loginToken.tenantId || tenantId, loginToken.tenants);
      await brandStore.loadBrand(loginToken.tenantId || tenantId);

      loginToken.userName = loginToken.userName || userName;
      loginToken.displayName = loginToken.displayName || loginToken.userName;
      const pass = await loginByToken(loginToken);

      if (pass) {
        const needRedirect = redirect && !checkTabClear();
        await redirectAfterAuthReady(needRedirect);

        window.$notification?.success({
          title: $t('page.login.loginSuccess'),
          content: $t('page.login.welcomeBack', { userName: userInfo.displayName || userInfo.userName }),
          duration: 4500
        });
      }
    } else {
      resetStore();
    }

    endLoading();
  }

  async function loginByOAuth2Ticket(ticket: string) {
    const { data: loginToken, error } = await fetchExchangeOAuth2Ticket(ticket);
    if (!error && loginToken) {
      const savedTenant = localStg.get('loginTenantId');
      persistTenantContext(loginToken.tenantId || savedTenant || undefined, loginToken.tenants);
      const tenant = loginToken.tenantId || savedTenant || undefined;
      if (tenant) {
        await brandStore.loadBrand(tenant);
      }
      return loginByToken(loginToken);
    }
    return false;
  }

  async function loginByToken(loginToken: Api.Auth.LoginToken) {
    persistLoginToken(loginToken);

    const pass = await getUserInfo(loginToken);

    if (pass) {
      token.value = loginToken.token;
      persistPlatformAdmin(loginToken.platformAdmin);
      return true;
    }

    return false;
  }

  /** Pick the first non-empty candidate that is not a JWT accidentally stored as a name. */
  function pickUserName(...candidates: Array<string | undefined | null>) {
    return (
      candidates.find(v => {
        const s = (v || '').trim();
        return s && !s.startsWith('eyJ');
      }) || ''
    );
  }

  /** Reload button permission codes from the backend and cache them. */
  async function refreshPermissions() {
    const { data: permissions, error } = await fetchMyPermissions();
    if (error || !permissions) {
      return false;
    }

    userInfo.buttons = extractButtonPermissionCodes(permissions);
    localStg.set('userButtons', userInfo.buttons);
    return true;
  }

  async function getUserInfo(loginToken?: Api.Auth.LoginToken) {
    const name = pickUserName(loginToken?.userName, userInfo.userName, localStg.get('userName'));
    const displayName =
      loginToken?.displayName || loginToken?.userName || userInfo.displayName || localStg.get('displayName') || name;

    if (name) {
      localStg.set('userName', name);
      localStg.set('displayName', displayName);
    }

    Object.assign(userInfo, {
      userId: name,
      userName: name,
      displayName,
      roles: ['USER'],
      buttons: userInfo.buttons
    });

    if (await refreshPermissions()) {
      return true;
    }

    // Permission fetch failed: keep the session, route init will reset the store if it keeps failing
    return Boolean(loginToken?.token || token.value);
  }

  async function initUserInfo() {
    const maybeToken = getToken();

    if (maybeToken) {
      token.value = maybeToken;
      const storedName = pickUserName(localStg.get('userName'));
      const storedDisplay = pickUserName(localStg.get('displayName'), storedName);
      if (storedName) {
        userInfo.userId = storedName;
        userInfo.userName = storedName;
        userInfo.displayName = storedDisplay;
      }
      const storedButtons = localStg.get('userButtons');
      if (storedButtons?.length) {
        userInfo.buttons = storedButtons;
      }

      platformAdmin.value = Boolean(localStg.get('platformAdmin'));
      const savedTenant = localStg.get('loginTenantId');
      if (savedTenant) {
        currentTenantId.value = savedTenant;
        await brandStore.loadBrand(savedTenant);
      }
      await loadMyTenants();

      const pass = await getUserInfo();

      if (!pass) {
        resetStore();
      }
    }
  }

  function persistPlatformAdmin(flag?: boolean) {
    platformAdmin.value = Boolean(flag);
    if (flag) {
      localStg.set('platformAdmin', true);
    } else {
      localStg.remove('platformAdmin');
    }
  }

  function persistTenantContext(tenantId?: string, nextTenants?: Api.Auth.TenantSelect[]) {
    if (tenantId) {
      currentTenantId.value = tenantId;
      localStg.set('loginTenantId', tenantId);
    }
    if (nextTenants?.length) {
      tenants.value = nextTenants;
    }
  }

  async function loadMyTenants() {
    const { data, error } = await fetchMyTenants();
    if (!error && data?.length) {
      tenants.value = data;
    }
  }

  async function switchTenant(tenantId: string) {
    if (!tenantId || tenantId === currentTenantId.value) {
      return;
    }
    pauseSessionEventsForTenantSwitch();
    const { data: loginToken, error } = await fetchSwitchTenant(tenantId);
    if (error || !loginToken) {
      resumeSessionEvents();
      return;
    }
    persistTenantContext(loginToken.tenantId || tenantId, loginToken.tenants);
    persistLoginToken(loginToken);
    persistPlatformAdmin(loginToken.platformAdmin);
    token.value = loginToken.token;
    window.location.reload();
  }

  /**
   * Register permission routes before leaving `/login`.
   * Jumping first matches `not-found`, then `resetVueRoutes` drops the login record mid-navigation → blank screen.
   */
  async function redirectAfterAuthReady(needRedirect = true) {
    const routeStore = useRouteStore();
    if (!routeStore.isInitAuthRoute) {
      await routeStore.initAuthRoute();
    }
    await nextTick();

    const redirect = router.currentRoute.value.query?.redirect as string;
    if (needRedirect && redirect) {
      await router.replace(redirect);
      return;
    }

    await router.replace(routeStore.homePath || '/403');
  }

  function hasPerm(code: string) {
    return userInfo.buttons.includes(code);
  }

  return {
    token,
    userInfo,
    tenants,
    currentTenantId,
    platformAdmin,
    isLogin,
    loginLoading,
    resetStore,
    login,
    loginByToken,
    loginByOAuth2Ticket,
    redirectAfterAuthReady,
    initUserInfo,
    refreshPermissions,
    switchTenant,
    loadMyTenants,
    hasPerm
  };
});
