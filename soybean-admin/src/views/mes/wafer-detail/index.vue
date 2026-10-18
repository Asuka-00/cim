<script setup lang="ts">
import { ref } from 'vue';
import { getWafer, type WaferRow } from '@/service/api/mes/wafer-query';
import { $t } from '@/locales';

defineOptions({ name: 'MesWaferDetail' });

const waferId = ref('');
const loading = ref(false);
const current = ref<WaferRow | null>(null);

async function fetchData() {
  const id = waferId.value.trim();
  if (!id) {
    window.$message?.warning($t('page.mes.waferDetail.required'));
    current.value = null;
    return;
  }
  loading.value = true;
  current.value = null;
  try {
    const res = await getWafer(id);
    if (res.error) return;
    current.value = res.data ?? null;
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.waferDetail.title') }}</span>
          <div class="flex items-center gap-8px">
            <NInput v-model:value="waferId" class="w-240px" :placeholder="$t('page.mes.waferDetail.waferId')" @keyup.enter="fetchData" />
            <NButton type="primary" :loading="loading" @click="fetchData">{{ $t('common.search') }}</NButton>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.waferDetail.hint') }}</div>
      <div v-if="current" class="flex flex-col gap-8px">
        <div>{{ $t('page.mes.waferDetail.code') }}：{{ current.waferCode }}</div>
        <div>{{ $t('page.mes.waferDetail.lotId') }}：{{ current.lotId }}</div>
        <div>{{ $t('page.mes.waferDetail.slot') }}：{{ current.slotNo }}</div>
        <div>{{ $t('page.mes.waferDetail.status') }}：{{ current.status }}</div>
        <div>{{ $t('page.mes.waferDetail.purpose') }}：{{ current.purpose }}</div>
        <div>{{ $t('page.mes.waferDetail.vendor') }}：{{ current.vendorWaferId }}</div>
        <div>{{ $t('page.mes.waferDetail.scrap') }}：{{ current.scrapReason }}</div>
        <div>{{ $t('page.mes.waferDetail.scrappedAt') }}：{{ current.scrappedAt }}</div>
        <div>{{ $t('page.mes.waferDetail.remark') }}：{{ current.remark }}</div>
      </div>
    </NCard>
  </div>
</template>
