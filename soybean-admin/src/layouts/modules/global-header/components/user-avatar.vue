<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import type { VNode } from 'vue';
import { useAuthStore } from '@/store/modules/auth';
import { useRouterPush } from '@/hooks/common/router';
import { useSvgIcon } from '@/hooks/common/icon';
import { $t } from '@/locales';
import ChangePasswordModal from './change-password-modal.vue';

defineOptions({
  name: 'UserAvatar'
});

const authStore = useAuthStore();
const { toLogin } = useRouterPush();
const { SvgIconVNode } = useSvgIcon();
const showChangePassword = ref(false);

function loginOrRegister() {
  toLogin();
}

function tenantLabel(item: Api.Auth.TenantSelect) {
  return item.tenantName ? `${item.tenantId} - ${item.tenantName}` : item.tenantId || item.id;
}

type DropdownOption =
  | {
      key: string;
      label: string;
      icon?: () => VNode;
      disabled?: boolean;
    }
  | {
      type: 'divider';
      key: string;
    }
  | {
      type: 'group';
      key: string;
      label: string;
      children: Array<{
        key: string;
        label: string;
        icon?: () => VNode;
        disabled?: boolean;
      }>;
    };

const options = computed(() => {
  const tenantChildren = authStore.tenants.length
    ? authStore.tenants.map(item => ({
        key: `tenant:${item.id}`,
        label: tenantLabel(item),
        icon: item.id === authStore.currentTenantId ? SvgIconVNode({ icon: 'ph:check', fontSize: 16 }) : undefined
      }))
    : [
        {
          key: 'tenant-empty',
          label: $t('page.layout.switchTenant'),
          disabled: true
        }
      ];

  const opts: DropdownOption[] = [
    {
      type: 'group',
      key: 'tenant-group',
      label: authStore.platformAdmin ? $t('page.layout.platformAdmin') : $t('page.layout.switchTenant'),
      children: tenantChildren
    },
    {
      type: 'divider',
      key: 'd-account'
    },
    {
      label: $t('page.systemManagement.user.changePassword'),
      key: 'change-password',
      icon: SvgIconVNode({ icon: 'ph:key', fontSize: 18 })
    },
    {
      label: $t('common.logout'),
      key: 'logout',
      icon: SvgIconVNode({ icon: 'ph:sign-out', fontSize: 18 })
    }
  ];

  return opts;
});

function logout() {
  window.$dialog?.info({
    title: $t('common.tip'),
    content: $t('common.logoutConfirm'),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: () => {
      authStore.resetStore();
    }
  });
}

function handleDropdown(key: string) {
  if (key === 'logout') {
    logout();
    return;
  }
  if (key === 'change-password') {
    showChangePassword.value = true;
    return;
  }
  if (key.startsWith('tenant:')) {
    authStore.switchTenant(key.slice(7));
  }
}

onMounted(() => {
  if (authStore.isLogin) {
    authStore.loadMyTenants();
  }
});
</script>

<template>
  <NButton v-if="!authStore.isLogin" quaternary @click="loginOrRegister">
    {{ $t('page.login.login') }}
  </NButton>
  <NDropdown v-else placement="bottom" trigger="click" :options="options" @select="handleDropdown">
    <div>
      <ButtonIcon>
        <SvgIcon icon="ph:user-circle" class="text-icon-large" />
        <span class="text-16px font-medium">{{ authStore.userInfo.displayName || authStore.userInfo.userName }}</span>
      </ButtonIcon>
    </div>
  </NDropdown>
  <ChangePasswordModal v-model:show="showChangePassword" />
</template>

<style scoped></style>
