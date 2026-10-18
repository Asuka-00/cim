<script setup lang="ts">
import { ref } from 'vue';
import { getLot, type Lot } from '@/service/api/mes/lot';
import { $t } from '@/locales';

defineOptions({ name: 'MesLotDetail' });

const lotId = ref('');
const loading = ref(false);
const current = ref<Lot | null>(null);

async function fetchData() {
  const id = lotId.value.trim();
  if (!id) {
    window.$message?.warning($t('page.mes.lotDetail.required'));
    current.value = null;
    return;
  }
  loading.value = true;
  current.value = null;
  try {
    const res = await getLot(id);
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
          <span>{{ $t('page.mes.lotDetail.title') }}</span>
          <div class="flex items-center gap-8px">
            <NInput v-model:value="lotId" class="w-240px" :placeholder="$t('page.mes.lotDetail.lotId')" @keyup.enter="fetchData" />
            <NButton type="primary" :loading="loading" @click="fetchData">{{ $t('common.search') }}</NButton>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.lotDetail.hint') }}</div>
      <div v-if="current" class="flex flex-col gap-8px">
        <div>{{ $t('page.mes.lotDetail.lotNo') }}：{{ current.lotNo }}</div>
        <div>{{ $t('page.mes.lotDetail.status') }}：{{ current.status }}</div>
        <div>{{ $t('page.mes.lotDetail.qty') }}：{{ current.qty }}</div>
        <div>{{ $t('page.mes.lotDetail.seq') }}：{{ current.routeSeqNo }}</div>
        <div>{{ $t('page.mes.lotDetail.pass') }}：{{ current.passNo }}</div>
        <div>{{ $t('page.mes.lotDetail.hold') }}：{{ current.holdCount }}</div>
        <div>{{ $t('page.mes.lotDetail.priority') }}：{{ current.priority }}</div>
        <div>{{ $t('page.mes.lotDetail.wafers') }}：{{ current.waferCount }}</div>
      </div>
    </NCard>
  </div>
</template>
