<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue';
import { NButton, type DataTableColumns } from 'naive-ui';
import { getSamplingCounters, resetSamplingCounter, type SamplingCounter } from '@/service/api/mes/sampling-counter';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { getProductList, type Product } from '@/service/api/mes/product';
import { getSamplingRules, type SamplingRule } from '@/service/api/mes/sampling-rule';
import { getStepList, type Step } from '@/service/api/mes/step';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesSamplingCounter' });

const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<SamplingCounter[]>([]);
const rules = ref<SamplingRule[]>([]);
const steps = ref<Step[]>([]);
const products = ref<Product[]>([]);
const lots = ref<Lot[]>([]);

const ruleName = computed(() => {
  const names = new Map(rules.value.map(item => [String(item.id), `${item.ruleCode} ${item.ruleName}`]));
  return (id: string) => names.get(String(id)) ?? id;
});
const stepName = computed(() => {
  const names = new Map(steps.value.map(item => [String(item.id), `${item.stepCode} ${item.stepName}`]));
  return (id: string) => names.get(String(id)) ?? id;
});
const productName = computed(() => {
  const names = new Map(products.value.map(item => [String(item.id), `${item.productCode} ${item.productName}`]));
  return (id?: string | number) => (!id || String(id) === '0' ? $t('page.mes.counter.any') : (names.get(String(id)) ?? String(id)));
});
const lotName = computed(() => {
  const names = new Map(lots.value.map(item => [String(item.id), item.lotNo]));
  return (id?: string | null) => (id ? (names.get(String(id)) ?? id) : '');
});

const columns = computed<DataTableColumns<SamplingCounter>>(() => [
  { title: $t('page.mes.counter.rule'), key: 'ruleId', render: row => ruleName.value(row.ruleId) },
  { title: $t('page.mes.counter.step'), key: 'stepId', render: row => stepName.value(row.stepId) },
  { title: $t('page.mes.counter.product'), key: 'productId', render: row => productName.value(row.productId) },
  { title: $t('page.mes.counter.count'), key: 'counter', width: 80 },
  { title: $t('page.mes.counter.lastLot'), key: 'lastSampledLotId', render: row => lotName.value(row.lastSampledLotId) },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 90,
    render: row =>
      hasAuth('mes:sampling-counter:reset')
        ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => handleReset(row) }, () => $t('page.mes.counter.reset'))
        : null
  }
]);

async function fetchData() {
  loading.value = true;
  try {
    const res = await getSamplingCounters();
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

function handleReset(row: SamplingCounter) {
  window.$dialog?.warning({
    title: $t('page.mes.counter.reset'),
    content: $t('page.mes.counter.resetConfirm'),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await resetSamplingCounter({
        ruleId: String(row.ruleId),
        stepId: String(row.stepId),
        routeStepId: !row.routeStepId || String(row.routeStepId) === '0' ? null : String(row.routeStepId),
        productId: !row.productId || String(row.productId) === '0' ? null : String(row.productId)
      });
      window.$message?.success($t('common.saveSuccess'));
      await fetchData();
    }
  });
}

onMounted(async () => {
  const [ruleRes, stepPage, productPage, lotPage] = await Promise.all([
    getSamplingRules(),
    getStepList({ page: 1, pageSize: 200 }),
    getProductList({ page: 1, pageSize: 200 }),
    getLotList({ page: 1, pageSize: 200 })
  ]);
  rules.value = ruleRes.data ?? [];
  steps.value = stepPage.data?.records ?? [];
  products.value = productPage.data?.records ?? [];
  lots.value = lotPage.data?.records ?? [];
  await fetchData();
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <span>{{ $t('page.mes.counter.title') }}</span>
      </template>
      <div class="mb-12px">{{ $t('page.mes.counter.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
  </div>
</template>
