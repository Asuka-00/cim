<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { getLotUda, getMissingLotUda, putLotUda } from '@/service/api/mes/uda';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesUda' });

const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string; status: string }>({
    initialQuery: { lotNo: '', status: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'lotNo', label: $t('page.mes.uda.lotNo'), type: 'input' }
]);

const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const values = ref<Array<{ attrCode: string; value: string }>>([]);
const missing = ref<string[]>([]);
const form = reactive({ attrCode: '', value: '' });

const rules = computed<FormRules>(() => ({
  attrCode: [{ required: true, message: $t('page.mes.uda.required'), trigger: 'blur' }],
  value: [{ required: true, message: $t('page.mes.uda.required'), trigger: 'blur' }]
}));

const valueColumns = computed<DataTableColumns<{ attrCode: string; value: string }>>(() => [
  { title: $t('page.mes.uda.attrCode'), key: 'attrCode' },
  { title: $t('page.mes.uda.value'), key: 'value' }
]);

async function loadUda(lotId: string) {
  const [valueRes, missingRes] = await Promise.all([getLotUda(lotId), getMissingLotUda(lotId)]);
  values.value = Object.entries(valueRes.data ?? {}).map(([attrCode, value]) => ({ attrCode, value }));
  missing.value = missingRes.data ?? [];
}

async function openUda(row: Lot) {
  current.value = row;
  form.attrCode = '';
  form.value = '';
  await loadUda(String(row.id));
  showModal.value = true;
}

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.uda.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.uda.status'), key: 'status', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 110,
    render: row =>
      hasAuth('mes:lot:uda')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openUda(row) }, () => $t('page.mes.uda.open'))
        : null
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  if (!current.value) return false;
  await putLotUda(String(current.value.id), { attrCode: form.attrCode.trim(), value: form.value });
  window.$message?.success($t('common.saveSuccess'));
  await loadUda(String(current.value.id));
  form.attrCode = '';
  form.value = '';
  return false;
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
        <span>{{ $t('page.mes.uda.title') }}</span>
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
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.uda.openTitle')" class="w-640px" :on-confirm="handleSubmit">
      <div v-if="missing.length" class="mb-12px">{{ $t('page.mes.uda.missing') }}：{{ missing.join('，') }}</div>
      <DataTable :columns="valueColumns" :data="values" size="small" class="mb-12px" />
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.uda.attrCode')" path="attrCode">
          <NInput v-model:value="form.attrCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.uda.value')" path="value">
          <NInput v-model:value="form.value" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
