<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { createUom, deleteUom, getUomList, updateUom, type Uom } from '@/service/api/mes/uom';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesUom' });

const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Uom, { uomCode: string }>({
    initialQuery: { uomCode: '' },
    fetcher: getUomList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'uomCode', label: $t('page.mes.uom.uomCode'), type: 'input' }
]);

const showModal = ref(false);
const editing = ref<Uom | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  uomCode: '',
  uomName: '',
  dimension: 'COUNT',
  decimals: 0,
  enabled: 1
});

const dimensionOptions = ['COUNT', 'VOLUME', 'WEIGHT', 'LENGTH', 'AREA', 'TIME', 'OTHER'].map(value => ({
  label: value,
  value
}));

const rules = computed<FormRules>(() => ({
  uomCode: [{ required: true, message: $t('page.mes.uom.required'), trigger: 'blur' }],
  uomName: [{ required: true, message: $t('page.mes.uom.required'), trigger: 'blur' }],
  dimension: [{ required: true, message: $t('page.mes.uom.required'), trigger: 'change' }]
}));

function openCreate() {
  editing.value = null;
  Object.assign(form, { uomCode: '', uomName: '', dimension: 'COUNT', decimals: 0, enabled: 1 });
  showModal.value = true;
}

function openEdit(row: Uom) {
  editing.value = row;
  Object.assign(form, row);
  showModal.value = true;
}

const columns = computed<DataTableColumns<Uom>>(() => [
  { title: $t('page.mes.uom.uomCode'), key: 'uomCode' },
  { title: $t('page.mes.uom.uomName'), key: 'uomName' },
  { title: $t('page.mes.uom.dimension'), key: 'dimension', width: 120 },
  { title: $t('page.mes.uom.decimals'), key: 'decimals', width: 100 },
  {
    title: $t('common.status'),
    key: 'enabled',
    width: 90,
    render: row => (row.enabled === 1 ? $t('common.enabled') : $t('common.disabled'))
  },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 160,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        hasAuth('mes:uom:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:uom:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  const payload = { ...form };
  if (editing.value) {
    await updateUom(String(editing.value.id), payload);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createUom(payload);
    window.$message?.success($t('common.addSuccess'));
  }
  await fetchData();
}

function handleDelete(row: Uom) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.uom.deleteConfirm', { name: row.uomCode }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteUom(String(row.id));
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
          <span>{{ $t('page.mes.uom.title') }}</span>
          <PermissionGuard code="mes:uom:add">
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
      :title="editing ? $t('page.mes.uom.editTitle') : $t('page.mes.uom.addTitle')"
      class="w-560px"
      :on-confirm="handleSubmit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.uom.uomCode')" path="uomCode">
          <NInput v-model:value="form.uomCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.uom.uomName')" path="uomName">
          <NInput v-model:value="form.uomName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.uom.dimension')" path="dimension">
          <NSelect v-model:value="form.dimension" :options="dimensionOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.uom.decimals')" path="decimals">
          <NInputNumber v-model:value="form.decimals" :min="0" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('common.status')" path="enabled">
          <NRadioGroup v-model:value="form.enabled">
            <NRadio :value="1">{{ $t('common.enabled') }}</NRadio>
            <NRadio :value="0">{{ $t('common.disabled') }}</NRadio>
          </NRadioGroup>
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
