<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules, type SelectOption } from 'naive-ui';
import { getUomList, type Uom } from '@/service/api/mes/uom';
import {
  createUomConversion,
  deleteUomConversion,
  getUomConversionList,
  resolveUomConversion,
  updateUomConversion,
  type UomConversion
} from '@/service/api/mes/uom-conversion';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesUomConversion' });

const { hasAuth } = useAuth();
const uoms = ref<Uom[]>([]);

const uomOptions = computed<SelectOption[]>(() => uoms.value.map(item => ({ label: item.uomCode, value: item.id })));

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<UomConversion, { fromUomId: string | null }, { fromUomId?: string }>({
    initialQuery: { fromUomId: null },
    buildQuery: query => (query.fromUomId ? { fromUomId: query.fromUomId } : {}),
    fetcher: getUomConversionList
  });

const filterFields = computed<FilterField[]>(() => [
  {
    key: 'fromUomId',
    label: $t('page.mes.uomConversion.fromUom'),
    type: 'select',
    options: uoms.value.map(item => ({ label: item.uomCode, value: item.id }))
  }
]);

const showModal = ref(false);
const editing = ref<UomConversion | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  fromUomId: null as string | null,
  toUomId: null as string | null,
  materialId: null as string | null,
  factor: null as number | null
});

const rules = computed<FormRules>(() => ({
  fromUomId: [{ required: true, message: $t('page.mes.uomConversion.required'), trigger: 'change' }],
  toUomId: [{ required: true, message: $t('page.mes.uomConversion.required'), trigger: 'change' }],
  factor: [{ required: true, type: 'number', message: $t('page.mes.uomConversion.required'), trigger: 'blur' }]
}));

function uomLabel(id: string) {
  return uoms.value.find(item => item.id === id)?.uomCode ?? id;
}

function openCreate() {
  editing.value = null;
  Object.assign(form, { fromUomId: null, toUomId: null, materialId: null, factor: null });
  showModal.value = true;
}

function openEdit(row: UomConversion) {
  editing.value = row;
  Object.assign(form, {
    fromUomId: row.fromUomId,
    toUomId: row.toUomId,
    materialId: row.materialId === '0' ? null : row.materialId,
    factor: row.factor
  });
  showModal.value = true;
}

const columns = computed<DataTableColumns<UomConversion>>(() => [
  { title: $t('page.mes.uomConversion.fromUom'), key: 'fromUomId', render: row => uomLabel(row.fromUomId) },
  { title: $t('page.mes.uomConversion.toUom'), key: 'toUomId', render: row => uomLabel(row.toUomId) },
  {
    title: $t('page.mes.uomConversion.materialId'),
    key: 'materialId',
    width: 120,
    render: row => (row.materialId === '0' ? $t('page.mes.uomConversion.generic') : row.materialId)
  },
  { title: $t('page.mes.uomConversion.factor'), key: 'factor', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 220,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        h(NButton, { size: 'small', quaternary: true, onClick: () => resolveRow(row) }, () => $t('page.mes.uomConversion.resolve')),
        hasAuth('mes:uom-conversion:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:uom-conversion:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);

function payload() {
  return {
    fromUomId: form.fromUomId!,
    toUomId: form.toUomId!,
    materialId: form.materialId || null,
    factor: form.factor!
  };
}

async function handleSubmit() {
  await formRef.value?.validate();
  if (editing.value) {
    await updateUomConversion(String(editing.value.id), payload());
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createUomConversion(payload());
    window.$message?.success($t('common.addSuccess'));
  }
  await fetchData();
}

async function resolveRow(row: UomConversion) {
  const { data, error } = await resolveUomConversion({
    fromUomId: row.fromUomId,
    toUomId: row.toUomId,
    materialId: row.materialId === '0' ? undefined : row.materialId
  });
  if (!error) {
    window.$message?.success(`${uomLabel(row.fromUomId)} → ${uomLabel(row.toUomId)} = ${data?.factor ?? ''}`);
  }
}

function handleDelete(row: UomConversion) {
  const name = `${uomLabel(row.fromUomId)} → ${uomLabel(row.toUomId)}`;
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.uomConversion.deleteConfirm', { name }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteUomConversion(String(row.id));
      window.$message?.success($t('common.deleteSuccess'));
      await fetchData();
    }
  });
}

onMounted(async () => {
  const { data } = await getUomList({ page: 1, pageSize: 200 });
  uoms.value = data?.records ?? [];
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
          <span>{{ $t('page.mes.uomConversion.title') }}</span>
          <PermissionGuard code="mes:uom-conversion:add">
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
      :title="editing ? $t('page.mes.uomConversion.editTitle') : $t('page.mes.uomConversion.addTitle')"
      class="w-560px"
      :on-confirm="handleSubmit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.uomConversion.fromUom')" path="fromUomId">
          <NSelect v-model:value="form.fromUomId" :options="uomOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.uomConversion.toUom')" path="toUomId">
          <NSelect v-model:value="form.toUomId" :options="uomOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.uomConversion.materialId')" path="materialId">
          <NInput v-model:value="form.materialId" :placeholder="$t('page.mes.uomConversion.generic')" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.uomConversion.factor')" path="factor">
          <NInputNumber v-model:value="form.factor" :min="0" class="w-full" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
