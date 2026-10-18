<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { reclaimWafer, reenterWafer } from '@/service/api/mes/wafer-reclaim';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { getLotWafers, type LotWafer } from '@/service/api/mes/split';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesWaferReclaim' });

const OPEN = new Set(['WAITING', 'RUNNING']);
const { hasAuth } = useAuth();
const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string }>({
    initialQuery: { lotNo: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [{ key: 'lotNo', label: $t('page.mes.reclaim.lotNo'), type: 'input' }]);
const showWafers = ref(false);
const showReenter = ref(false);
const formRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const source = ref<LotWafer | null>(null);
const wafers = ref<LotWafer[]>([]);
const targets = ref<Lot[]>([]);
const form = reactive({ lotId: null as string | null, waferCode: '' });

const rules = computed<FormRules>(() => ({
  lotId: [{ required: true, message: $t('page.mes.reclaim.required'), trigger: 'change' }],
  waferCode: [{ required: true, message: $t('page.mes.reclaim.required'), trigger: 'blur' }]
}));
const targetOptions = computed(() =>
  targets.value.filter(item => OPEN.has(item.status)).map(item => ({ label: item.lotNo, value: String(item.id) }))
);

async function openWafers(row: Lot) {
  current.value = row;
  const res = await getLotWafers(String(row.id));
  wafers.value = res.data ?? [];
  showWafers.value = true;
}

function openReenter(row: LotWafer) {
  source.value = row;
  Object.assign(form, { lotId: null, waferCode: '' });
  showReenter.value = true;
}

const lotColumns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.reclaim.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.reclaim.status'), key: 'status', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 90,
    render: row =>
      h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openWafers(row) }, () => $t('page.mes.reclaim.wafers'))
  }
]);

function canReclaim(row: LotWafer) {
  return hasAuth('mes:wafer:reclaim') && (row.status === 'SCRAPPED' || current.value?.status === 'TERMINATED');
}

const waferColumns = computed<DataTableColumns<LotWafer>>(() => [
  { title: $t('page.mes.reclaim.wafer'), key: 'waferCode' },
  { title: $t('page.mes.reclaim.status'), key: 'status', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 160,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        canReclaim(row)
          ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => handleReclaim(row) }, () => $t('page.mes.reclaim.reclaim'))
          : null,
        row.status === 'RECLAIMED' && hasAuth('mes:wafer:reenter')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openReenter(row) }, () => $t('page.mes.reclaim.reenter'))
          : null
      ])
  }
]);

async function refreshWafers() {
  if (!current.value) return;
  const res = await getLotWafers(String(current.value.id));
  wafers.value = res.data ?? [];
}

function handleReclaim(row: LotWafer) {
  window.$dialog?.warning({
    title: $t('page.mes.reclaim.reclaim'),
    content: $t('page.mes.reclaim.reclaimConfirm', { name: row.waferCode }),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await reclaimWafer(String(row.id));
      window.$message?.success($t('common.saveSuccess'));
      await refreshWafers();
    }
  });
}

async function submitReenter() {
  await formRef.value?.validate();
  if (!source.value || !form.lotId) return;
  await reenterWafer(form.lotId, { reclaimedWaferId: String(source.value.id), waferCode: form.waferCode });
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

onMounted(async () => {
  const page = await getLotList({ page: 1, pageSize: 200 });
  targets.value = page.data?.records ?? [];
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
        <span>{{ $t('page.mes.reclaim.title') }}</span>
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
    <CrudDialog v-model:show="showWafers" :title="$t('page.mes.reclaim.wafers')" class="w-720px" hide-footer>
      <div class="mb-12px">{{ $t('page.mes.reclaim.hint') }}</div>
      <DataTable :columns="waferColumns" :data="wafers" size="small" />
    </CrudDialog>
    <CrudDialog v-model:show="showReenter" :title="$t('page.mes.reclaim.reenterTitle')" class="w-560px" :on-confirm="submitReenter">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.reclaim.target')" path="lotId">
          <NSelect v-model:value="form.lotId" :options="targetOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.reclaim.newCode')" path="waferCode">
          <NInput v-model:value="form.waferCode" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
