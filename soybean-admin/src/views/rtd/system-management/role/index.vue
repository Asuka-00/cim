<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules, type TreeOption } from 'naive-ui';
import {
  createRole,
  deleteRole,
  getRoleList,
  getRolePermissions,
  updateRole,
  type RoleQuery,
  type RoleUpdateRequestDTO,
  type RoleVO
} from '@/service/api/rtd/system-management/role';
import { getPermissionOptions, type PermissionVO } from '@/service/api/rtd/system-management/permission';
import { CommonState, type CommonStateValue } from '@/service/api/common';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { useI18n } from 'vue-i18n';
import { $t } from '@/locales';
import { translatePermissionName } from '@/locales/permission';
import { renderEnabledTag } from '@/utils/tag-render';

defineOptions({ name: 'RtdSystemManagementRole' });

const { hasAuth } = useAuth();
const { locale } = useI18n();

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
} = usePageQuery<RoleVO, RoleQuery>({
  initialQuery: { roleCode: '', roleName: '', enabled: undefined },
  fetcher: getRoleList
});

const filterFields = computed<FilterField[]>(() => [
  { key: 'roleCode', label: $t('page.systemManagement.role.roleCode'), type: 'input' },
  { key: 'roleName', label: $t('page.systemManagement.role.roleName'), type: 'input' },
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

interface PermissionTreeNode extends TreeOption {
  key: string;
  label: string;
  type?: string;
  children?: PermissionTreeNode[];
}

const showModal = ref(false);
const editing = ref<RoleVO | null>(null);
const formRef = ref<FormInst | null>(null);
const rawPermissions = ref<PermissionVO[]>([]);
const checkedKeys = ref<string[]>([]);
const initialPermissionIds = ref<string[]>([]);
const treeLoading = ref(false);
const permissionTree = computed(() => {
  void locale.value;
  return buildPermissionTree(rawPermissions.value);
});
const form = reactive({
  roleCode: '',
  roleName: '',
  enabled: CommonState.ENABLED as CommonStateValue
});

const rules = computed<FormRules>(() => ({
  roleCode: [{ required: true, message: $t('page.systemManagement.role.roleCodeRequired'), trigger: 'blur' }],
  roleName: [{ required: true, message: $t('page.systemManagement.role.roleNameRequired'), trigger: 'blur' }]
}));

const columns = computed<DataTableColumns<RoleVO>>(() => [
  {
    title: '#',
    key: 'index',
    width: 60,
    render: (_r, i) => (currentPage.value - 1) * pageSize.value + i + 1
  },
  { title: $t('page.systemManagement.role.roleCode'), key: 'roleCode', ellipsis: { tooltip: true } },
  { title: $t('page.systemManagement.role.roleName'), key: 'roleName', ellipsis: { tooltip: true } },
  {
    title: $t('common.status'),
    key: 'enabled',
    width: 90,
    render: row => renderEnabledTag(row.enabled)
  },
  { title: $t('common.updateBy'), key: 'updateBy', width: 120 },
  { title: $t('common.updateAt'), key: 'updateAt', width: 180 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 160,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        hasAuth('rtd:role:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () =>
              $t('common.edit')
            )
          : null,
        hasAuth('rtd:role:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () =>
              $t('common.delete')
            )
          : null
      ])
  }
]);

function sameStringSet(left?: string[] | null, right?: string[] | null): boolean {
  const a = [...new Set((left ?? []).map(s => s.trim()).filter(Boolean))].sort();
  const b = [...new Set((right ?? []).map(s => s.trim()).filter(Boolean))].sort();
  return a.length === b.length && a.every((v, i) => v === b[i]);
}

function buildPermissionTree(permissions: PermissionVO[]): PermissionTreeNode[] {
  const map = new Map<string | null, PermissionVO[]>();
  permissions.forEach(p => {
    const parentId = p.parentId ?? null;
    if (!map.has(parentId)) map.set(parentId, []);
    map.get(parentId)!.push(p);
  });

  function walk(parentId: string | null): PermissionTreeNode[] {
    const children = map.get(parentId) ?? [];
    return children.map(p => ({
      key: p.id,
      label: translatePermissionName(p.permissionName),
      type: p.type,
      children: walk(p.id)
    }));
  }

  return walk(null);
}

function onCheckedKeysUpdate(keys: Array<string | number>) {
  checkedKeys.value = keys.map(String);
}

async function loadPermissionTreeForCreate() {
  treeLoading.value = true;
  try {
    const res = await getPermissionOptions();
    rawPermissions.value = res.data ?? [];
    checkedKeys.value = [];
    initialPermissionIds.value = [];
  } finally {
    treeLoading.value = false;
  }
}

async function loadPermissionTreeForEdit(roleId: string) {
  treeLoading.value = true;
  try {
    const res = await getRolePermissions(roleId);
    rawPermissions.value = [...(res.data?.includedPermissions ?? []), ...(res.data?.excludedPermissions ?? [])];
    const includedIds = (res.data?.includedPermissions ?? []).map(p => p.id);
    checkedKeys.value = [...includedIds];
    initialPermissionIds.value = [...includedIds];
  } finally {
    treeLoading.value = false;
  }
}

async function openCreate() {
  editing.value = null;
  Object.assign(form, { roleCode: '', roleName: '', enabled: CommonState.ENABLED });
  showModal.value = true;
  await loadPermissionTreeForCreate();
}

async function openEdit(row: RoleVO) {
  editing.value = row;
  Object.assign(form, { roleCode: row.roleCode, roleName: row.roleName, enabled: row.enabled });
  showModal.value = true;
  await loadPermissionTreeForEdit(row.id);
}

async function handleSubmit() {
  await formRef.value?.validate();
  if (editing.value) {
    const payload: RoleUpdateRequestDTO = {
      roleName: form.roleName,
      enabled: form.enabled
    };
    if (!sameStringSet(initialPermissionIds.value, checkedKeys.value)) {
      payload.permissionIds = [...checkedKeys.value];
    }
    await updateRole(editing.value.id, payload);
    window.$message?.success($t('common.updateSuccess'));
  } else {
    await createRole({
      roleCode: form.roleCode,
      roleName: form.roleName,
      enabled: form.enabled,
      permissionIds: [...checkedKeys.value]
    });
    window.$message?.success($t('common.addSuccess'));
  }
  await fetchData();
}

function handleDelete(row: RoleVO) {
  window.$dialog?.warning({
    title: $t('page.systemManagement.common.confirmDeleteTitle'),
    content: $t('page.systemManagement.role.deleteConfirm', { name: row.roleName }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteRole(row.id);
      window.$message?.success($t('common.deleteSuccess'));
      await fetchData();
    }
  });
}

onMounted(fetchData);
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
          <span>{{ $t('page.systemManagement.role.title') }}</span>
          <PermissionGuard code="rtd:role:add">
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
      :title="editing ? $t('page.systemManagement.role.editTitle') : $t('page.systemManagement.role.addTitle')"
      class="w-560px"
      :on-confirm="handleSubmit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="auto">
        <NFormItem :label="$t('page.systemManagement.role.roleCode')" path="roleCode">
          <NInput v-model:value="form.roleCode" :disabled="Boolean(editing)" />
        </NFormItem>
        <NFormItem :label="$t('page.systemManagement.role.roleName')" path="roleName">
          <NInput v-model:value="form.roleName" />
        </NFormItem>
        <NFormItem :label="$t('common.status')" path="enabled">
          <NRadioGroup v-model:value="form.enabled">
            <NRadio :value="CommonState.ENABLED">{{ $t('common.enabled') }}</NRadio>
            <NRadio :value="CommonState.DISABLED">{{ $t('common.disabled') }}</NRadio>
          </NRadioGroup>
        </NFormItem>
        <NFormItem :label="$t('page.systemManagement.role.permissions')">
          <NSpin :show="treeLoading" class="w-full">
            <div class="max-h-320px w-full overflow-y-auto border border-base-text/12 rounded-4px p-8px">
              <NTree
                block-line
                checkable
                cascade
                expand-on-click
                :data="permissionTree"
                key-field="key"
                label-field="label"
                children-field="children"
                :checked-keys="checkedKeys"
                @update:checked-keys="onCheckedKeysUpdate"
              />
            </div>
          </NSpin>
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
