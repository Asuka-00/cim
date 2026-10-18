<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getWaferMaps, importWaferMap, type WaferMap } from '@/service/api/mes/wafer-map';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { getLotWafers, type LotWafer } from '@/service/api/mes/split';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesWaferMap' });

const { hasAuth } = useAuth();
const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string }>({
    initialQuery: { lotNo: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [{ key: 'lotNo', label: $t('page.mes.waferMap.lotNo'), type: 'input' }]);
const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const wafers = ref<LotWafer[]>([]);
const maps = ref<WaferMap[]>([]);
const form = reactive({ waferId: null as string | null, fileName: '', csv: '' });

const rules = computed<FormRules>(() => ({
  waferId: [{ required: true, message: $t('page.mes.waferMap.required'), trigger: 'change' }],
  csv: [{ required: true, message: $t('page.mes.waferMap.required'), trigger: 'blur' }]
}));

const waferName = computed(() => {
  const names = new Map(wafers.value.map(item => [String(item.id), item.waferCode]));
  return (id: string) => names.get(String(id)) ?? id;
});
const waferOptions = computed(() => wafers.value.map(item => ({ label: item.waferCode, value: String(item.id) })));

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.waferMap.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.waferMap.status'), key: 'status', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 110,
    render: row =>
      hasAuth('mes:wafer-map:import')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openLot(row) }, () =>
            $t('page.mes.waferMap.import')
          )
        : null
  }
]);

const mapColumns = computed<DataTableColumns<WaferMap>>(() => [
  { title: $t('page.mes.waferMap.wafer'), key: 'waferId', render: row => waferName.value(String(row.waferId)) },
  { title: $t('page.mes.waferMap.version'), key: 'mapVersion', width: 80 },
  { title: $t('page.mes.waferMap.current'), key: 'isCurrent', width: 80, render: row => (row.isCurrent === 1 ? $t('page.mes.waferMap.yes') : '') },
  { title: $t('page.mes.waferMap.totalDie'), key: 'totalDie', width: 90 },
  { title: $t('page.mes.waferMap.goodDie'), key: 'goodDie', width: 90 }
]);

async function loadMaps(lotId: string) {
  const res = await getWaferMaps(lotId);
  maps.value = res.data ?? [];
}

async function openLot(row: Lot) {
  current.value = row;
  Object.assign(form, { waferId: null, fileName: '', csv: '' });
  const waferRes = await getLotWafers(String(row.id));
  wafers.value = waferRes.data ?? [];
  await loadMaps(String(row.id));
  showModal.value = true;
}

async function handleSubmit() {
  await formRef.value?.validate();
  if (!current.value || !form.waferId) return false;
  const res = await importWaferMap(String(current.value.id), {
    waferId: form.waferId,
    fileName: form.fileName,
    csv: form.csv
  });
  window.$message?.success($t('page.mes.waferMap.imported', { version: res.data ?? '' }));
  form.csv = '';
  await loadMaps(String(current.value.id));
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
        <span>{{ $t('page.mes.waferMap.title') }}</span>
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
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.waferMap.importTitle')" class="w-720px" :on-confirm="handleSubmit">
      <DataTable :columns="mapColumns" :data="maps" size="small" class="mb-12px" />
      <div class="mb-12px">{{ $t('page.mes.waferMap.csvHint') }}</div>
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.waferMap.wafer')" path="waferId">
          <NSelect v-model:value="form.waferId" :options="waferOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.waferMap.fileName')" path="fileName">
          <NInput v-model:value="form.fileName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.waferMap.csv')" path="csv">
          <NInput v-model:value="form.csv" type="textarea" :rows="6" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
