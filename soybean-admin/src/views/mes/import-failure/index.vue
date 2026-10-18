<script setup lang="ts">
import { computed, ref } from 'vue';
import type { DataTableColumns } from 'naive-ui';
import { getImportFailures, getImportTasksByType, type ImportErrorRow, type ImportTask } from '@/service/api/mes/import-task';
import { $t } from '@/locales';

defineOptions({ name: 'MesImportFailure' });

const importType = ref('WIP_INIT');
const tasks = ref<ImportTask[]>([]);
const taskId = ref<string | null>(null);
const loading = ref(false);
const rows = ref<ImportErrorRow[]>([]);
const taskOptions = computed(() =>
  tasks.value.map(item => ({ label: `${item.taskNo} ${item.status}`, value: String(item.id) }))
);
const columns = computed<DataTableColumns<ImportErrorRow>>(() => [
  { title: $t('page.mes.importFailure.rowNo'), key: 'rowNo', width: 90 },
  { title: $t('page.mes.importFailure.code'), key: 'errorCode', width: 160 },
  { title: $t('page.mes.importFailure.message'), key: 'message' },
  { title: $t('page.mes.importFailure.raw'), key: 'rawData' }
]);

async function loadTasks() {
  taskId.value = null;
  rows.value = [];
  const type = importType.value.trim();
  if (!type) {
    tasks.value = [];
    return;
  }
  const res = await getImportTasksByType(type);
  tasks.value = res.data ?? [];
}

async function fetchData() {
  if (!taskId.value) {
    rows.value = [];
    return;
  }
  loading.value = true;
  try {
    const res = await getImportFailures(taskId.value);
    rows.value = res.data ?? [];
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
          <span>{{ $t('page.mes.importFailure.title') }}</span>
          <div class="flex items-center gap-8px">
            <NInput v-model:value="importType" class="w-180px" @keyup.enter="loadTasks" />
            <NButton @click="loadTasks">{{ $t('page.mes.importFailure.loadTasks') }}</NButton>
            <NSelect v-model:value="taskId" class="w-260px" :options="taskOptions" filterable @update:value="fetchData" />
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.importFailure.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
  </div>
</template>
