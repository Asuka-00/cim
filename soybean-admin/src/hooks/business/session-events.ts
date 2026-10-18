import { onBeforeUnmount, watch } from 'vue';
import { useAuthStore } from '@/store/modules/auth';
import { useRouteStore } from '@/store/modules/route';
import { $t } from '@/locales';
import { bindSessionSse, shouldIgnoreForceLogout } from './sse-session';
import { SseEventType, useSse } from './sse';

interface ForceLogoutPayload {
  reason?: string;
}

/**
 * Keep the SSE channel in sync with the login state and route server pushes to the stores.
 *
 * Mount once in the authenticated layout.
 */
export function useSessionEvents() {
  const authStore = useAuthStore();
  const routeStore = useRouteStore();
  const sse = useSse();
  bindSessionSse(sse);

  sse.on<ForceLogoutPayload>(SseEventType.FORCE_LOGOUT, payload => {
    if (shouldIgnoreForceLogout()) {
      return;
    }
    window.$dialog?.warning({
      title: $t('page.layout.forceLogout.title'),
      content: payload?.reason || $t('page.layout.forceLogout.defaultReason'),
      positiveText: $t('common.confirm'),
      onPositiveClick: () => {
        sse.disconnect();
        authStore.resetStore();
      }
    });
  });

  sse.on(SseEventType.PERMISSION_CHANGED, async () => {
    try {
      await authStore.refreshPermissions();
      await routeStore.reloadAuthRoutes();
      window.$message?.success($t('page.layout.permissionRefreshed'));
    } catch (error) {
      console.error('[sse] permission refresh failed', error);
      window.$message?.error($t('page.layout.permissionRefreshFailed'));
    }
  });

  watch(
    () => authStore.isLogin,
    isLogin => {
      if (isLogin) {
        sse.connect();
      } else {
        sse.disconnect();
      }
    },
    { immediate: true }
  );

  onBeforeUnmount(() => sse.disconnect());

  return sse;
}
