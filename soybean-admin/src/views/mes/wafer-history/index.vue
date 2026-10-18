<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import type { DataTableColumns } from 'naive-ui';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { getLotWafers, type LotWafer } from '@/service/api/mes/split';
import { getWaferHistory, type WaferHistory } from '@/service/api/mes/wafer-history';
import { $t } from '@/locales';

defineOptions({ name: 'MesWaferHistory' });

const lots = ref<Lot[]>([]);
const wafers = ref<LotWafer[]>([]);
const lotId = ref<string | null>(null);
const waferId = ref<string | null>(null);
const loading = ref(false);
const rows = ref<WaferHistory[]>([]);

const lotOptions = computed(() => lots.value.map(item => ({ label: item.lotNo, value: String(item.id) })));
const waferOptions = computed(() =>
  wafers.value.map(item => ({ label: `${item.slotNo ?? ''} ${item.waferCode}`.trim(), value: String(item.id) }))
);
const columns = computed<DataTableColumns<WaferHistory>>(() => [
  { title: $t('page.mes.waferHistory.time'), key: 'occurredAt', width: 180 },
  { title: $t('page.mes.waferHistory.type'), key: 'txnType', width: 140 },
  { title: $t('page.mes.waferHistory.seq'), key: 'routeSeqNo', width: 80 },
  { title: $t('page.mes.waferHistory.slotBefore'), key: 'slotNoBefore', width: 90 },
  { title: $t('page.mes.waferHistory.slotAfter'), key: 'slotNoAfter', width: 90 },
  { title: $t('page.mes.waferHistory.statusBefore'), key: 'statusBefore', width: 110 },
  { title: $t('page.mes.waferHistory.statusAfter'), key: 'statusAfter', width: 110 },
  { title: $t('page.mes.waferHistory.reason'), key: 'reasonCode' },
  { title: $t('page.mes.waferHistory.operator'), key: 'operator', width: 120 }
]);

async function onLotChange(value: string | null) {
  waferId.value = null;
  wafers.value = [];
  rows.value = [];
  if (!value) return;
  const res = await getLotWafers(value);
  wafers.value = res.data ?? [];
}

async function fetchData() {
  if (!waferId.value) {
    rows.value = [];
    return;
  }
  loading.value = true;
  try {
    const res = await getWaferHistory(waferId.value);
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  const page = await getLotList({ page: 1, pageSize: 200 });
  lots.value = page.data?.records ?? [];
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.waferHistory.title') }}</span>
          <div class="flex items-center gap-8px">
            <NSelect v-model:value="lotId" class="w-220px" :options="lotOptions" filterable @update:value="onLotChange" />
            <NSelect v-model:value="waferId" class="w-220px" :options="waferOptions" :disabled="!lotId" filterable @update:value="fetchData" />
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.waferHistory.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
  </div>
</template>
