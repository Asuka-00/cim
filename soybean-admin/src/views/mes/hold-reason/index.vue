<script setup lang="ts">
import { computed, h, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { createHoldReason, deleteHoldReason, getHoldReasonList, updateHoldReason, type HoldReason } from '@/service/api/mes/hold';
import CrudDialog from '@/components/business/crud-dialog.vue';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { usePageQuery } from '@/hooks/business/page-query';
import { $t } from '@/locales';

defineOptions({ name: 'MesHoldReason' });

const TYPES = ['QUALITY', 'EQUIPMENT', 'PROCESS', 'ENGINEERING', 'MATERIAL', 'OTHER'];
const { hasAuth } = useAuth();
const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<HoldReason, { reasonCode: string }>({
    initialQuery: { reasonCode: '' },
    fetcher: getHoldReasonList
  });

const filterFields = computed<FilterField[]>(() => [{ key: 'reasonCode', label: $t('page.mes.holdReason.code'), type: 'input' }]);
const showModal = ref(false);
const editing = ref<HoldReason | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({ reasonCode: '', reasonName: '', holdType: 'OTHER', releaseRoleCode: '', remark: '', enabled: true });
const rules = computed<FormRules>(() => ({
  reasonCode: [{ required: true, message: $t('page.mes.holdReason.required'), trigger: 'blur' }],
  reasonName: [{ required: true, message: $t('page.mes.holdReason.required'), trigger: 'blur' }]
}));
const typeOptions = TYPES.map(item => ({ label: item, value: item }));

function openCreate() {
  editing.value = null;
  Object.assign(form, { reasonCode: '', reasonName: '', holdType: 'OTHER', releaseRoleCode: '', remark: '', enabled: true });
  showModal.value = true;
}

function openEdit(row: HoldReason) {
  editing.value = row;
  Object.assign(form, {
    reasonCode: row.reasonCode,
    reasonName: row.reasonName,
    holdType: row.holdType || 'OTHER',
    releaseRoleCode: row.releaseRoleCode ?? '',
    remark: row.remark ?? '',
    enabled: row.enabled !== 0
  });
  showModal.value = true;
}

const columns = computed<DataTableColumns<HoldReason>>(() => [
  { title: $t('page.mes.holdReason.code'), key: 'reasonCode', width: 160 },
  { title: $t('page.mes.holdReason.name'), key: 'reasonName' },
  { title: $t('page.mes.holdReason.type'), key: 'holdType', width: 140 },
  { title: $t('page.mes.holdReason.releaseRole'), key: 'releaseRoleCode', width: 160 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 140,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        hasAuth('mes:hold-reason:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:hold-reason:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);

async function submit() {
  await formRef.value?.validate();
  const data = {
    reasonCode: form.reasonCode,
    reasonName: form.reasonName,
    holdType: form.holdType,
    releaseRoleCode: form.releaseRoleCode,
    remark: form.remark,
    enabled: form.enabled ? 1 : 0
  };
  if (editing.value) await updateHoldReason(String(editing.value.id), data);
  else await createHoldReason(data);
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

function handleDelete(row: HoldReason) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.holdReason.deleteConfirm', { name: row.reasonCode }),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteHoldReason(String(row.id));
      window.$message?.success($t('common.saveSuccess'));
      await fetchData();
    }
  });
}
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.holdReason.title') }}</span>
          <PermissionGuard code="mes:hold-reason:add">
            <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
          </PermissionGuard>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.holdReason.hint') }}</div>
      <FilterBar v-model="searchParams" :fields="filterFields" @search="handleSearch" @reset="handleReset" />
      <DataTable flex-height :columns="columns" :data="list" :loading="loading" :bordered="false" size="small" />
      <div class="mt-12px flex justify-end">
        <NPagination
          :page="currentPage"
          :page-size="pageSize"
          :item-count="total"
          show-size-picker
          @update:page="onPageUpdate"
          @update:page-size="onSizeUpdate"
        />
      </div>
    </NCard>
    <CrudDialog
      v-model:show="showModal"
      :title="editing ? $t('page.mes.holdReason.editTitle') : $t('page.mes.holdReason.addTitle')"
      class="w-560px"
      :on-confirm="submit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="110">
        <NFormItem :label="$t('page.mes.holdReason.code')" path="reasonCode">
          <NInput v-model:value="form.reasonCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.holdReason.name')" path="reasonName">
          <NInput v-model:value="form.reasonName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.holdReason.type')" path="holdType">
          <NSelect v-model:value="form.holdType" :options="typeOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.holdReason.releaseRole')" path="releaseRoleCode">
          <NInput v-model:value="form.releaseRoleCode" />
        </NFormItem>
        <NFormItem :label="$t('common.remark')" path="remark">
          <NInput v-model:value="form.remark" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.holdReason.enabled')" path="enabled">
          <NSwitch v-model:value="form.enabled" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
