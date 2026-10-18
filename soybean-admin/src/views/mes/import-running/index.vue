<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import type { DataTableColumns } from 'naive-ui';
import { getRunningImportTasks, type ImportTask } from '@/service/api/mes/import-task';
import { $t } from '@/locales';

defineOptions({ name: 'MesImportRunning' });

const loading = ref(false);
const rows = ref<ImportTask[]>([]);
const columns = computed<DataTableColumns<ImportTask>>(() => [
  { title: $t('page.mes.importRunning.taskNo'), key: 'taskNo', width: 160 },
  { title: $t('page.mes.importRunning.type'), key: 'importType', width: 140 },
  { title: $t('page.mes.importRunning.operator'), key: 'operator', width: 140 },
  { title: $t('page.mes.importRunning.total'), key: 'totalRows', width: 90 },
  { title: $t('page.mes.importRunning.success'), key: 'successRows', width: 90 },
  { title: $t('page.mes.importRunning.fail'), key: 'failRows', width: 90 },
  { title: $t('page.mes.importRunning.startedAt'), key: 'startedAt', width: 180 }
]);

async function fetchData() {
  loading.value = true;
  try {
    const res = await getRunningImportTasks();
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.importRunning.title') }}</span>
          <NButton @click="fetchData">{{ $t('page.mes.importRunning.reload') }}</NButton>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.importRunning.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
  </div>
</template>
