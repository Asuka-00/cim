<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { type DataTableColumns } from 'naive-ui';
import { getDailyOutput, getDailyOutputByProduct, type DailyOutput, type ProductOutput } from '@/service/api/mes/daily-output';
import { getProductList, type Product } from '@/service/api/mes/product';
import { $t } from '@/locales';

defineOptions({ name: 'MesDailyOutput' });

function today() {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

const loading = ref(false);
const date = ref<string | null>(today());
const summary = ref<DailyOutput>({ lotCount: 0, qty: 0, scrapQty: 0 });
const rows = ref<ProductOutput[]>([]);
const products = ref<Product[]>([]);

const productName = computed(() => {
  const names = new Map(products.value.map(item => [String(item.id), `${item.productCode} ${item.productName}`]));
  return (id?: string | null) => (id ? (names.get(String(id)) ?? id) : $t('page.mes.daily.noProduct'));
});

const columns = computed<DataTableColumns<ProductOutput>>(() => [
  { title: $t('page.mes.daily.product'), key: 'productId', render: row => productName.value(row.productId) },
  { title: $t('page.mes.daily.lotCount'), key: 'lotCount', width: 120 },
  { title: $t('page.mes.daily.qty'), key: 'qty', width: 120 },
  { title: $t('page.mes.daily.scrapQty'), key: 'scrapQty', width: 120 }
]);

async function fetchData() {
  if (!date.value) return;
  loading.value = true;
  try {
    const [sumRes, productRes] = await Promise.all([getDailyOutput(date.value), getDailyOutputByProduct(date.value)]);
    summary.value = sumRes.data ?? { lotCount: 0, qty: 0, scrapQty: 0 };
    rows.value = productRes.data ?? [];
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  const page = await getProductList({ page: 1, pageSize: 200 });
  products.value = page.data?.records ?? [];
  await fetchData();
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.daily.title') }}</span>
          <div class="flex items-center gap-8px">
            <NDatePicker v-model:formatted-value="date" type="date" value-format="yyyy-MM-dd" />
            <NButton type="primary" @click="fetchData">{{ $t('common.search') }}</NButton>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.daily.hint') }}</div>
      <div class="mb-12px">
        {{ $t('page.mes.daily.summary', { lots: summary.lotCount, qty: summary.qty, scrap: summary.scrapQty }) }}
      </div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
  </div>
</template>
