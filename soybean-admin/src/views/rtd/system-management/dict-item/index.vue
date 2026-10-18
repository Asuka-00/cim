<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import {
  createDictItem,
  deleteDictItem,
  getDictItemList,
  updateDictItem,
  type DictItemQuery,
  type DictItemVO
} from '@/service/api/rtd/system-management/dict-item';
import { getAllDictTypes, type DictTypeVO } from '@/service/api/rtd/system-management/dict-type';
import { CommonState, type CommonStateValue } from '@/service/api/common';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';
import { renderEnabledTag } from '@/utils/tag-render';

defineOptions({ name: 'RtdSystemManagementDictItem' });

const { hasAuth } = useAuth();
const dictTypeOptions = ref<{ label: string; value: string }[]>([]);

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
} = usePageQuery<DictItemVO, DictItemQuery>({
  initialQuery: { dictType: '', itemCode: '', itemLabel: '', enabled: undefined },
  fetcher: getDictItemList
});

const filterFields = computed<FilterField[]>(() => [
  {
    key: 'dictType',
    label: $t('page.systemManagement.common.dictType'),
    type: 'select',
    options: dictTypeOptions.value
  },
  { key: 'itemCode', label: $t('page.systemManagement.dictItem.itemCode'), type: 'input' },
  { key: 'itemLabel', label: $t('page.systemManagement.dictItem.itemLabel'), type: 'input' },
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
const editing = ref<DictItemVO | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  dictType: '',
  itemCode: '',
  labelZh: '',
  labelEn: '',
  sortOrder: 0 as number | null,
  cssClass: '',
  extraJson: '',
  enabled: CommonState.ENABLED as CommonStateValue
});

const rules = computed<FormRules>(() => ({
  dictType: [{ required: true, message: $t('page.systemManagement.dictItem.dictTypeRequired'), trigger: 'change' }],
  itemCode: [{ required: true, message: $t('page.systemManagement.dictItem.itemCodeRequired'), trigger: 'blur' }],
  labelZh: [{ required: true, message: $t('page.systemManagement.dictItem.labelZhRequired'), trigger: 'blur' }]
}));

const columns = computed<DataTableColumns<DictItemVO>>(() => [
  {
    title: '#',
    key: 'index',
    width: 60,
    render: (_r, i) => (currentPage.value - 1) * pageSize.value + i + 1
  },
  { title: $t('page.systemManagement.common.dictType'), key: 'dictType', width: 140 },
  { title: $t('page.systemManagement.dictItem.itemCode'), key: 'itemCode', width: 120 },
  { title: $t('page.systemManagement.dictItem.labelZh'), key: 'itemLabel', ellipsis: { tooltip: true } },
  {
    title: $t('page.systemManagement.dictItem.labelEn'),
    key: 'labels',
    width: 120,
    render: row => row.labels?.en ?? ''
  },
  { title: $t('common.sort'), key: 'sortOrder', width: 70 },
  {
    title: $t('common.status'),
    key: 'enabled',
    width: 90,
    render: row => renderEnabledTag(row.enabled)
  },
  { title: $t('common.updateBy'), key: 'updateBy', width: 100 },
  { title: $t('common.updateAt'), key: 'updateAt', width: 170 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 160,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        hasAuth('rtd:dict-item:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () =>
              $t('common.edit')
            )
          : null,
        hasAuth('rtd:dict-item:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () =>
              $t('common.delete')
            )
          : null
      ])
  }
]);

function openCreate() {
  editing.value = null;
  Object.assign(form, {
    dictType: '',
    itemCode: '',
    labelZh: '',
    labelEn: '',
    sortOrder: 0,
    cssClass: '',
    extraJson: '',
    enabled: CommonState.ENABLED
  });
  showModal.value = true;
}

function openEdit(row: DictItemVO) {
  editing.value = row;
  Object.assign(form, {
    dictType: row.dictType,
    itemCode: row.itemCode,
    labelZh: row.labels?.zh || row.itemLabel || '',
    labelEn: row.labels?.en || '',
    sortOrder: row.sortOrder ?? 0,
    cssClass: row.cssClass ?? '',
    extraJson: row.extraJson ?? '',
    enabled: row.enabled
  });
  showModal.value = true;
}

async function handleSubmit() {
  await formRef.value?.validate();
  const labels = { zh: form.labelZh, en: form.labelEn || undefined };
  if (editing.value) {
    await updateDictItem(editing.value.id, {
      itemLabel: form.labelZh,
      labels,
      sortOrder: form.sortOrder ?? undefined,
      cssClass: form.cssClass || undefined,
      extraJson: form.extraJson || undefined,
      enabled: form.enabled
    });
    window.$message?.success($t('common.updateSuccess'));
  } else {
    await createDictItem({
      dictType: form.dictType,
      itemCode: form.itemCode,
      itemLabel: form.labelZh,
      labels,
      sortOrder: form.sortOrder ?? undefined,
      cssClass: form.cssClass || undefined,
      extraJson: form.extraJson || undefined,
      enabled: form.enabled
    });
    window.$message?.success($t('common.addSuccess'));
  }
  await fetchData();
}

function handleDelete(row: DictItemVO) {
  window.$dialog?.warning({
    title: $t('page.systemManagement.common.confirmDeleteTitle'),
    content: $t('page.systemManagement.dictItem.deleteConfirm', { name: row.itemCode }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteDictItem(row.id);
      window.$message?.success($t('common.deleteSuccess'));
      await fetchData();
    }
  });
}

async function loadDictTypes() {
  const res = await getAllDictTypes();
  dictTypeOptions.value = (res.data ?? []).map((dt: DictTypeVO) => ({
    label: `${dt.dictName} (${dt.dictType})`,
    value: dt.dictType
  }));
}

onMounted(async () => {
  await loadDictTypes();
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
          <span>{{ $t('page.systemManagement.dictItem.title') }}</span>
          <PermissionGuard code="rtd:dict-item:add">
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
      :title="editing ? $t('page.systemManagement.dictItem.editTitle') : $t('page.systemManagement.dictItem.addTitle')"
      class="w-560px"
      :on-confirm="handleSubmit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="auto">
        <NFormItem :label="$t('page.systemManagement.common.dictType')" path="dictType">
          <NSelect v-model:value="form.dictType" :options="dictTypeOptions" filterable :disabled="Boolean(editing)" />
        </NFormItem>
        <NFormItem :label="$t('page.systemManagement.dictItem.itemCode')" path="itemCode">
          <NInput v-model:value="form.itemCode" :disabled="Boolean(editing)" />
        </NFormItem>
        <NFormItem :label="$t('page.systemManagement.dictItem.labelZh')" path="labelZh">
          <NInput v-model:value="form.labelZh" />
        </NFormItem>
        <NFormItem :label="$t('page.systemManagement.dictItem.labelEn')" path="labelEn">
          <NInput v-model:value="form.labelEn" />
        </NFormItem>
        <NFormItem :label="$t('common.sort')" path="sortOrder">
          <NInputNumber v-model:value="form.sortOrder" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.systemManagement.dictItem.cssClass')" path="cssClass">
          <NInput v-model:value="form.cssClass" />
        </NFormItem>
        <NFormItem :label="$t('page.systemManagement.dictItem.extraJson')" path="extraJson">
          <NInput v-model:value="form.extraJson" type="textarea" :rows="2" />
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
