<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import {
  adjustConsumption,
  getLotConsumptions,
  settleConsumption,
  type LotConsumption
} from '@/service/api/mes/consumption';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { getMaterialList, type Material } from '@/service/api/mes/material';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesConsumptionAdjust' });

const { hasAuth } = useAuth();
const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string }>({
    initialQuery: { lotNo: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [{ key: 'lotNo', label: $t('page.mes.adjust.lotNo'), type: 'input' }]);
const showRows = ref(false);
const showAdjust = ref(false);
const formRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const selected = ref<LotConsumption | null>(null);
const rows = ref<LotConsumption[]>([]);
const materials = ref<Material[]>([]);
const form = reactive({ qtyAfter: null as number | null });

const rules = computed<FormRules>(() => ({
  qtyAfter: [{ required: true, type: 'number', message: $t('page.mes.adjust.required'), trigger: 'blur' }]
}));
const materialName = computed(() => {
  const names = new Map(materials.value.map(item => [String(item.id), `${item.materialCode} ${item.materialName}`]));
  return (id: string) => names.get(String(id)) ?? id;
});

async function openRows(row: Lot) {
  current.value = row;
  const res = await getLotConsumptions(String(row.id));
  rows.value = res.data ?? [];
  showRows.value = true;
}

function openAdjust(row: LotConsumption) {
  selected.value = row;
  form.qtyAfter = Number(row.qty);
  showAdjust.value = true;
}

const lotColumns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.adjust.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.adjust.status'), key: 'status', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 90,
    render: row =>
      h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openRows(row) }, () => $t('page.mes.adjust.rows'))
  }
]);

const rowColumns = computed<DataTableColumns<LotConsumption>>(() => [
  { title: $t('page.mes.adjust.material'), key: 'materialId', render: row => materialName.value(row.materialId) },
  { title: $t('page.mes.adjust.qty'), key: 'qty', width: 100 },
  { title: $t('page.mes.adjust.source'), key: 'source', width: 120 },
  { title: $t('page.mes.adjust.shortage'), key: 'shortage', width: 80, render: row => (row.shortage ? $t('page.mes.adjust.yes') : '') },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 140,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        row.shortage && hasAuth('mes:consumption:settle')
          ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => handleSettle(row) }, () => $t('page.mes.adjust.settle'))
          : null,
        !row.shortage && hasAuth('mes:consumption:adjust')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openAdjust(row) }, () => $t('page.mes.adjust.adjust'))
          : null
      ])
  }
]);

async function refreshRows() {
  if (!current.value) return;
  const res = await getLotConsumptions(String(current.value.id));
  rows.value = res.data ?? [];
}

function handleSettle(row: LotConsumption) {
  window.$dialog?.warning({
    title: $t('page.mes.adjust.settle'),
    content: $t('page.mes.adjust.settleConfirm'),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await settleConsumption(String(row.id));
      window.$message?.success($t('common.saveSuccess'));
      await refreshRows();
    }
  });
}

async function submitAdjust() {
  await formRef.value?.validate();
  if (!selected.value || form.qtyAfter == null) return;
  await adjustConsumption(String(selected.value.id), { qtyAfter: form.qtyAfter });
  window.$message?.success($t('common.saveSuccess'));
  await refreshRows();
}

onMounted(async () => {
  const page = await getMaterialList({ page: 1, pageSize: 200 });
  materials.value = page.data?.records ?? [];
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
        <span>{{ $t('page.mes.adjust.title') }}</span>
      </template>
      <DataTable flex-height :columns="lotColumns" :data="list" :loading="loading" :bordered="false" size="small" />
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
    <CrudDialog v-model:show="showRows" :title="$t('page.mes.adjust.rows')" class="w-860px" hide-footer>
      <div class="mb-12px">{{ $t('page.mes.adjust.hint') }}</div>
      <DataTable :columns="rowColumns" :data="rows" size="small" />
    </CrudDialog>
    <CrudDialog v-model:show="showAdjust" :title="$t('page.mes.adjust.adjustTitle')" class="w-480px" :on-confirm="submitAdjust">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="110">
        <NFormItem :label="$t('page.mes.adjust.qtyAfter')" path="qtyAfter">
          <NInputNumber v-model:value="form.qtyAfter" :min="0" class="w-full" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
