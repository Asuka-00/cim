<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import {
  createDictType,
  deleteDictType,
  getDictTypeList,
  updateDictType,
  type DictTypeQuery,
  type DictTypeVO
} from '@/service/api/rtd/system-management/dict-type';
import { CommonState, type CommonStateValue } from '@/service/api/common';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';
import { renderEnabledTag } from '@/utils/tag-render';

defineOptions({ name: 'RtdSystemManagementDictType' });

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
} = usePageQuery<DictTypeVO, DictTypeQuery>({
  initialQuery: { dictType: '', dictName: '', enabled: undefined },
  fetcher: getDictTypeList
});

const filterFields = computed<FilterField[]>(() => [
  { key: 'dictType', label: $t('page.systemManagement.common.dictType'), type: 'input' },
  { key: 'dictName', label: $t('page.systemManagement.dictType.dictName'), type: 'input' },
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
const editing = ref<DictTypeVO | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  dictType: '',
  dictName: '',
  remark: '',
  enabled: CommonState.ENABLED as CommonStateValue
});

const rules = computed<FormRules>(() => ({
  dictType: [{ required: true, message: $t('page.systemManagement.dictType.dictTypeRequired'), trigger: 'blur' }],
  dictName: [{ required: true, message: $t('page.systemManagement.dictType.dictNameRequired'), trigger: 'blur' }]
}));

const columns = computed<DataTableColumns<DictTypeVO>>(() => [
  {
    title: '#',
    key: 'index',
    width: 60,
    render: (_r, i) => (currentPage.value - 1) * pageSize.value + i + 1
  },
  { title: $t('page.systemManagement.common.dictType'), key: 'dictType', ellipsis: { tooltip: true } },
  { title: $t('page.systemManagement.dictType.dictName'), key: 'dictName', ellipsis: { tooltip: true } },
  { title: $t('common.remark'), key: 'remark', ellipsis: { tooltip: true } },
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
        hasAuth('rtd:dict-type:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () =>
              $t('common.edit')
            )
          : null,
        hasAuth('rtd:dict-type:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () =>
              $t('common.delete')
            )
          : null
      ])
  }
]);

function openCreate() {
  editing.value = null;
  Object.assign(form, { dictType: '', dictName: '', remark: '', enabled: CommonState.ENABLED });
  showModal.value = true;
}

function openEdit(row: DictTypeVO) {
  editing.value = row;
  Object.assign(form, {
    dictType: row.dictType,
    dictName: row.dictName,
    remark: row.remark ?? '',
    enabled: row.enabled
  });
  showModal.value = true;
}

async function handleSubmit() {
  await formRef.value?.validate();
  if (editing.value) {
    await updateDictType(editing.value.id, {
      dictName: form.dictName,
      remark: form.remark || undefined,
      enabled: form.enabled
    });
    window.$message?.success($t('common.updateSuccess'));
  } else {
    await createDictType({
      dictType: form.dictType,
      dictName: form.dictName,
      remark: form.remark || undefined,
      enabled: form.enabled
    });
    window.$message?.success($t('common.addSuccess'));
  }
  await fetchData();
}

function handleDelete(row: DictTypeVO) {
  window.$dialog?.warning({
    title: $t('page.systemManagement.common.confirmDeleteTitle'),
    content: $t('page.systemManagement.dictType.deleteConfirm', { name: row.dictType }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteDictType(row.id);
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
          <span>{{ $t('page.systemManagement.dictType.title') }}</span>
          <PermissionGuard code="rtd:dict-type:add">
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
      :title="editing ? $t('page.systemManagement.dictType.editTitle') : $t('page.systemManagement.dictType.addTitle')"
      class="w-520px"
      :on-confirm="handleSubmit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="auto">
        <NFormItem :label="$t('page.systemManagement.common.dictType')" path="dictType">
          <NInput v-model:value="form.dictType" :disabled="Boolean(editing)" />
        </NFormItem>
        <NFormItem :label="$t('page.systemManagement.dictType.dictName')" path="dictName">
          <NInput v-model:value="form.dictName" />
        </NFormItem>
        <NFormItem :label="$t('common.remark')" path="remark">
          <NInput v-model:value="form.remark" type="textarea" :rows="2" />
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
