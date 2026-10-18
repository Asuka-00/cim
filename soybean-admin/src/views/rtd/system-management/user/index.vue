<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import {
  createUser,
  deleteUser,
  getUser,
  getUserList,
  resetUserPassword,
  updateUser,
  type UserQuery,
  type UserTenantMembership,
  type UserVO
} from '@/service/api/rtd/system-management/user';
import { getRoleOptions, type RoleVO } from '@/service/api/rtd/system-management/role';
import { getTenantDropdown } from '@/service/api/rtd/system-management/tenant';
import { CommonState, type CommonStateValue } from '@/service/api/common';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { useAuthStore } from '@/store/modules/auth';
import { $t } from '@/locales';
import { renderBoolTag, renderEnabledTag } from '@/utils/tag-render';
import { localStg } from '@/utils/storage';

defineOptions({ name: 'RtdSystemManagementUser' });

const { hasAuth } = useAuth();
const authStore = useAuthStore();
const canEditPlatformAdmin = computed(() => authStore.platformAdmin);
const tenantOptions = ref<{ label: string; value: string }[]>([]);
const roleOptionsByTenant = ref<Record<string, { label: string; value: string }[]>>({});

const {
  loading,
  list,
  total,
  currentPage,
  pageSize,
  searchParams,
  fetchData,
  handleSearch,
  handleReset,
  onPageUpdate,
  onSizeUpdate
} = usePageQuery<UserVO, UserQuery>({
  initialQuery: { userName: '', displayName: '', enabled: undefined },
  fetcher: getUserList
});

const filterFields = computed<FilterField[]>(() => [
  { key: 'userName', label: $t('page.systemManagement.user.userName'), type: 'input' },
  { key: 'displayName', label: $t('page.systemManagement.user.displayName'), type: 'input' },
  {
    key: 'enabled',
    label: $t('common.status'),
    type: 'select',
    options: [
      { label: $t('common.enabled'), value: CommonState.ENABLED },
      { label: $t('common.disabled'), value: CommonState.DISABLED }
    ]
  }
]);

const showModal = ref(false);
const editing = ref<UserVO | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  userName: '',
  displayName: '',
  password: '',
  enabled: CommonState.ENABLED as CommonStateValue,
  platformAdmin: false,
  tenantIds: [] as string[],
  rolesByTenant: {} as Record<string, string[]>
});

const disableSelfPlatformAdmin = computed(
  () => Boolean(editing.value) && form.userName === authStore.userInfo.userName && form.platformAdmin
);

const rules = computed<FormRules>(() => ({
  userName: [{ required: true, message: $t('page.systemManagement.user.userNameRequired'), trigger: 'blur' }],
  displayName: [{ required: true, message: $t('page.systemManagement.user.displayNameRequired'), trigger: 'blur' }],
  password: editing.value
    ? []
    : [{ required: true, message: $t('page.systemManagement.user.passwordRequired'), trigger: 'blur' }],
  tenantIds: [
    { type: 'array', required: true, message: $t('page.systemManagement.user.tenantRequired'), trigger: 'change' }
  ]
}));

const columns = computed<DataTableColumns<UserVO>>(() => [
  {
    title: '#',
    key: 'index',
    width: 60,
    render: (_r, i) => (currentPage.value - 1) * pageSize.value + i + 1
  },
  { title: $t('page.systemManagement.user.userName'), key: 'userName', ellipsis: { tooltip: true } },
  { title: $t('page.systemManagement.user.displayName'), key: 'displayName', ellipsis: { tooltip: true } },
  {
    title: $t('page.systemManagement.user.tenants'),
    key: 'tenantNames',
    ellipsis: { tooltip: true },
    render: row => row.tenantNames || '-'
  },
  {
    title: $t('common.status'),
    key: 'enabled',
    width: 90,
    render: row => renderEnabledTag(row.enabled)
  },
  ...(canEditPlatformAdmin.value
    ? ([
        {
          title: $t('page.systemManagement.user.platformAdmin'),
          key: 'platformAdmin',
          width: 100,
          render: (row: UserVO) => renderBoolTag(Boolean(row.platformAdmin))
        }
      ] as DataTableColumns<UserVO>)
    : []),
  { title: $t('common.updateBy'), key: 'updateBy', width: 120 },
  { title: $t('common.updateAt'), key: 'updateAt', width: 180 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 240,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        hasAuth('rtd:user:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () =>
              $t('common.edit')
            )
          : null,
        hasAuth('rtd:user:reset-password')
          ? h(
              NButton,
              { size: 'small', quaternary: true, type: 'warning', onClick: () => handleResetPassword(row) },
              () => $t('page.systemManagement.user.resetPassword')
            )
          : null,
        hasAuth('rtd:user:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () =>
              $t('common.delete')
            )
          : null
      ])
  }
]);

function defaultFormState() {
  const current = localStg.get('loginTenantId') || '';
  return {
    userName: '',
    displayName: '',
    password: '',
    enabled: CommonState.ENABLED as CommonStateValue,
    platformAdmin: false,
    tenantIds: current ? [current] : ([] as string[]),
    rolesByTenant: {} as Record<string, string[]>
  };
}

function toMemberships(): UserTenantMembership[] {
  return form.tenantIds.map(tenantId => ({
    tenantId,
    roleIds: form.rolesByTenant[tenantId] ?? []
  }));
}

function tenantLabel(tenantId: string) {
  return tenantOptions.value.find(item => item.value === tenantId)?.label || tenantId;
}

async function ensureRoleOptions(tenantId: string) {
  if (roleOptionsByTenant.value[tenantId]) {
    return;
  }
  const res = await getRoleOptions(tenantId);
  roleOptionsByTenant.value[tenantId] = (res.data ?? []).map((r: RoleVO) => ({
    label: `${r.roleName} (${r.roleCode})`,
    value: r.id
  }));
}

async function onTenantIdsChange(ids: string[]) {
  form.tenantIds = ids;
  const next: Record<string, string[]> = {};
  for (const id of ids) {
    next[id] = form.rolesByTenant[id] ?? [];
    await ensureRoleOptions(id);
  }
  form.rolesByTenant = next;
}

function openCreate() {
  editing.value = null;
  Object.assign(form, defaultFormState());
  showModal.value = true;
  form.tenantIds.forEach(id => ensureRoleOptions(id));
}

async function openEdit(row: UserVO) {
  editing.value = row;
  const { data: detail, error } = await getUser(row.userName);
  if (error || !detail) return;
  const memberships = detail.memberships ?? [];
  const rolesByTenant: Record<string, string[]> = {};
  const tenantIds = memberships.map(item => {
    rolesByTenant[item.tenantId] = item.roleIds ?? [];
    return item.tenantId;
  });
  Object.assign(form, {
    userName: detail.userName,
    displayName: detail.displayName,
    password: '',
    enabled: detail.enabled,
    platformAdmin: Boolean(detail.platformAdmin),
    tenantIds,
    rolesByTenant
  });
  showModal.value = true;
  await Promise.all(tenantIds.map(id => ensureRoleOptions(id)));
}

async function handleSubmit() {
  await formRef.value?.validate();
  const memberships = toMemberships();
  if (memberships.some(item => !item.roleIds.length)) {
    window.$message?.error($t('page.systemManagement.user.tenantRoleRequired'));
    return false;
  }
  if (editing.value) {
    await updateUser(editing.value.id, {
      displayName: form.displayName,
      enabled: form.enabled,
      platformAdmin: canEditPlatformAdmin.value ? form.platformAdmin : undefined,
      memberships
    });
    window.$message?.success($t('common.updateSuccess'));
  } else {
    await createUser({
      userName: form.userName,
      displayName: form.displayName,
      password: form.password,
      enabled: form.enabled,
      platformAdmin: canEditPlatformAdmin.value ? form.platformAdmin : undefined,
      memberships
    });
    window.$message?.success($t('common.addSuccess'));
  }
  await fetchData();
}

function handleDelete(row: UserVO) {
  window.$dialog?.warning({
    title: $t('page.systemManagement.common.confirmDeleteTitle'),
    content: $t('page.systemManagement.user.deleteConfirm', { name: row.userName }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteUser(row.id);
      window.$message?.success($t('common.deleteSuccess'));
      await fetchData();
    }
  });
}

function handleResetPassword(row: UserVO) {
  window.$dialog?.warning({
    title: $t('page.systemManagement.user.confirmResetPasswordTitle'),
    content: $t('page.systemManagement.user.resetPasswordConfirm', { name: row.userName }),
    positiveText: $t('common.reset'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await resetUserPassword(row.userName);
      window.$message?.success($t('page.systemManagement.user.resetPasswordSuccess'));
    }
  });
}

async function loadTenants() {
  const res = await getTenantDropdown();
  tenantOptions.value = (res.data ?? []).map(item => ({
    label: `${item.tenantId} - ${item.tenantName}`,
    value: item.id
  }));
}

onMounted(async () => {
  await loadTenants();
  await fetchData();
});
</script>

<template>
  <div class="rtd-page">
    <FilterBar
      :fields="filterFields"
      :model-value="searchParams as Record<string, unknown>"
      @update:model-value="v => Object.assign(searchParams, v)"
      @search="handleSearch"
      @reset="handleReset"
    />
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.systemManagement.user.title') }}</span>
          <PermissionGuard code="rtd:user:add">
            <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
          </PermissionGuard>
        </div>
      </template>
      <DataTable flex-height :columns="columns" :data="list" :loading="loading" :bordered="false" size="small" />
      <div class="mt-12px flex justify-end">
        <NPagination
          :page="currentPage"
          :page-size="pageSize"
          :item-count="total"
          show-size-picker
          :page-sizes="[10, 20, 50]"
          @update:page="onPageUpdate"
          @update:page-size="onSizeUpdate"
        />
      </div>
    </NCard>

    <CrudDialog
      v-model:show="showModal"
      :title="editing ? $t('page.systemManagement.user.editTitle') : $t('page.systemManagement.user.addTitle')"
      class="w-640px"
      :on-confirm="handleSubmit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="auto">
        <NFormItem :label="$t('page.systemManagement.user.userName')" path="userName">
          <NInput v-model:value="form.userName" :disabled="Boolean(editing)" />
        </NFormItem>
        <NFormItem :label="$t('page.systemManagement.user.displayName')" path="displayName">
          <NInput v-model:value="form.displayName" />
        </NFormItem>
        <NFormItem v-if="!editing" :label="$t('page.systemManagement.user.password')" path="password">
          <NInput v-model:value="form.password" type="password" show-password-on="click" />
        </NFormItem>
        <NFormItem :label="$t('page.systemManagement.user.tenants')" path="tenantIds">
          <NSelect
            :value="form.tenantIds"
            multiple
            filterable
            :options="tenantOptions"
            @update:value="onTenantIdsChange"
          />
        </NFormItem>
        <NFormItem
          v-for="tenantId in form.tenantIds"
          :key="tenantId"
          :label="$t('page.systemManagement.user.tenantRole', { name: tenantLabel(tenantId) })"
        >
          <NSelect
            v-model:value="form.rolesByTenant[tenantId]"
            multiple
            filterable
            :options="roleOptionsByTenant[tenantId] ?? []"
          />
        </NFormItem>
        <NFormItem :label="$t('common.status')" path="enabled">
          <NRadioGroup v-model:value="form.enabled">
            <NRadio :value="CommonState.ENABLED">{{ $t('common.enabled') }}</NRadio>
            <NRadio :value="CommonState.DISABLED">{{ $t('common.disabled') }}</NRadio>
          </NRadioGroup>
        </NFormItem>
        <NFormItem
          v-if="canEditPlatformAdmin"
          :label="$t('page.systemManagement.user.platformAdmin')"
          path="platformAdmin"
        >
          <NSwitch v-model:value="form.platformAdmin" :disabled="disableSelfPlatformAdmin" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
