<script setup lang="ts">
import { ref } from 'vue';
import { getDefaultPrintTemplate, type PrintTemplateDefault } from '@/service/api/mes/print-template';
import { $t } from '@/locales';

defineOptions({ name: 'MesPrintDefault' });

const TYPES = ['LOT_TRAVELER', 'CARRIER_LABEL', 'WAFER_LABEL', 'STRIP_LABEL', 'REEL_LABEL', 'MATERIAL_LABEL'];
const tplType = ref('LOT_TRAVELER');
const loading = ref(false);
const current = ref<PrintTemplateDefault | null>(null);
const typeOptions = TYPES.map(item => ({ label: item, value: item }));

async function fetchData() {
  loading.value = true;
  current.value = null;
  try {
    const res = await getDefaultPrintTemplate(tplType.value);
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
          <span>{{ $t('page.mes.printDefault.title') }}</span>
          <div class="flex items-center gap-8px">
            <NSelect v-model:value="tplType" class="w-220px" :options="typeOptions" />
            <NButton type="primary" :loading="loading" @click="fetchData">{{ $t('common.search') }}</NButton>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.printDefault.hint') }}</div>
      <div v-if="current" class="flex flex-col gap-12px">
        <div>{{ $t('page.mes.printDefault.code') }}：{{ current.tplCode }}</div>
        <div>{{ $t('page.mes.printDefault.name') }}：{{ current.tplName }}</div>
        <NInput :value="current.content" type="textarea" :rows="12" readonly />
      </div>
    </NCard>
  </div>
</template>
