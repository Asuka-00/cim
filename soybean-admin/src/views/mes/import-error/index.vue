<script setup lang="ts">
import { ref } from 'vue';
import { getImportFailure, type ImportErrorRow } from '@/service/api/mes/import-task';
import { $t } from '@/locales';

defineOptions({ name: 'MesImportError' });

const errorId = ref('');
const loading = ref(false);
const current = ref<ImportErrorRow | null>(null);

async function fetchData() {
  const id = errorId.value.trim();
  if (!id) {
    window.$message?.warning($t('page.mes.importError.required'));
    current.value = null;
    return;
  }
  loading.value = true;
  current.value = null;
  try {
    const res = await getImportFailure(id);
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
          <span>{{ $t('page.mes.importError.title') }}</span>
          <div class="flex items-center gap-8px">
            <NInput v-model:value="errorId" class="w-240px" :placeholder="$t('page.mes.importError.errorId')" @keyup.enter="fetchData" />
            <NButton type="primary" :loading="loading" @click="fetchData">{{ $t('common.search') }}</NButton>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.importError.hint') }}</div>
      <div v-if="current" class="flex flex-col gap-8px">
        <div>{{ $t('page.mes.importError.rowNo') }}：{{ current.rowNo }}</div>
        <div>{{ $t('page.mes.importError.code') }}：{{ current.errorCode }}</div>
        <div>{{ $t('page.mes.importError.message') }}：{{ current.message }}</div>
        <NInput :value="current.rawData ?? ''" type="textarea" :rows="8" readonly />
      </div>
    </NCard>
  </div>
</template>
