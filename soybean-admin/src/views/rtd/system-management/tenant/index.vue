<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import {
  createTenant,
  deleteTenant,
  getTenantList,
  updateTenant,
  type TenantQuery,
  type TenantVO
} from '@/service/api/rtd/system-management/tenant';
import { CommonState, type CommonStateValue } from '@/service/api/common';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';
import { renderEnabledTag } from '@/utils/tag-render';

defineOptions({ name: 'RtdSystemManagementTenant' });

const { hasAuth } = useAuth();

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
} = usePageQuery<TenantVO, TenantQuery>({
  initialQuery: { tenantId: '', tenantName: '', enabled: undefined },
  fetcher: getTenantList
});

const filterFields = computed<FilterField[]>(() => [
  { key: 'tenantId', label: $t('page.systemManagement.tenant.tenantId'), type: 'input' },
  { key: 'tenantName', label: $t('page.systemManagement.tenant.tenantName'), type: 'input' },
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
const editing = ref<TenantVO | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  tenantId: '',
  tenantName: '',
  enabled: CommonState.ENABLED as CommonStateValue
});

const rules = computed<FormRules>(() => ({
  tenantId: [{ required: true, message: $t('page.systemManagement.tenant.tenantIdRequired'), trigger: 'blur' }],
  tenantName: [{ required: true, message: $t('page.systemManagement.tenant.tenantNameRequired'), trigger: 'blur' }]
}));

const columns = computed<DataTableColumns<TenantVO>>(() => [
  {
    title: '#',
    key: 'index',
    width: 60,
    render: (_r, i) => (currentPage.value - 1) * pageSize.value + i + 1
  },
  { title: $t('page.systemManagement.tenant.tenantId'), key: 'tenantId', ellipsis: { tooltip: true } },
  { title: $t('page.systemManagement.tenant.tenantName'), key: 'tenantName', ellipsis: { tooltip: true } },
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
        hasAuth('rtd:tenant:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () =>
              $t('common.edit')
            )
          : null,
        hasAuth('rtd:tenant:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () =>
              $t('common.delete')
            )
          : null
      ])
  }
]);

function openCreate() {
  editing.value = null;
  Object.assign(form, { tenantId: '', tenantName: '', enabled: CommonState.ENABLED });
  showModal.value = true;
}

function openEdit(row: TenantVO) {
  editing.value = row;
  Object.assign(form, { tenantId: row.tenantId, tenantName: row.tenantName, enabled: row.enabled });
  showModal.value = true;
}

async function handleSubmit() {
  await formRef.value?.validate();
  if (editing.value) {
    await updateTenant(editing.value.id, { tenantName: form.tenantName, enabled: form.enabled });
    window.$message?.success($t('common.updateSuccess'));
  } else {
    await createTenant({ tenantId: form.tenantId, tenantName: form.tenantName, enabled: form.enabled });
    window.$message?.success($t('common.addSuccess'));
  }
  await fetchData();
}

function handleDelete(row: TenantVO) {
  window.$dialog?.warning({
    title: $t('page.systemManagement.common.confirmDeleteTitle'),
    content: $t('page.systemManagement.tenant.deleteConfirm', { name: row.tenantName }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteTenant(row.id);
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
          <span>{{ $t('page.systemManagement.tenant.title') }}</span>
          <PermissionGuard code="rtd:tenant:add">
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
      :title="editing ? $t('page.systemManagement.tenant.editTitle') : $t('page.systemManagement.tenant.addTitle')"
      class="w-480px"
      :on-confirm="handleSubmit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="auto">
        <NFormItem :label="$t('page.systemManagement.tenant.tenantId')" path="tenantId">
          <NInput v-model:value="form.tenantId" :disabled="Boolean(editing)" />
        </NFormItem>
        <NFormItem :label="$t('page.systemManagement.tenant.tenantName')" path="tenantName">
          <NInput v-model:value="form.tenantName" />
        </NFormItem>
        <NFormItem :label="$t('common.status')" path="enabled">
          <NRadioGroup v-model:value="form.enabled">
            <NRadio :value="CommonState.ENABLED">{{ $t('common.enabled') }}</NRadio>
            <NRadio :value="CommonState.DISABLED">{{ $t('common.disabled') }}</NRadio>
          </NRadioGroup>
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
