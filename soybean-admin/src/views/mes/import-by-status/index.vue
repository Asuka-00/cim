<script setup lang="ts">
import { computed, ref } from 'vue';
import type { DataTableColumns } from 'naive-ui';
import { getImportTasksByStatus, type ImportTask } from '@/service/api/mes/import-task';
import { $t } from '@/locales';

defineOptions({ name: 'MesImportByStatus' });

const STATUSES = ['DRAFT', 'PENDING', 'RUNNING', 'SUCCESS', 'PARTIAL', 'FAILED', 'CANCELLED'];
const status = ref('RUNNING');
const importType = ref('');
const loading = ref(false);
const rows = ref<ImportTask[]>([]);
const statusOptions = STATUSES.map(item => ({ label: item, value: item }));
const columns = computed<DataTableColumns<ImportTask>>(() => [
  { title: $t('page.mes.importByStatus.taskNo'), key: 'taskNo', width: 160 },
  { title: $t('page.mes.importByStatus.type'), key: 'importType', width: 140 },
  { title: $t('page.mes.importByStatus.operator'), key: 'operator', width: 140 },
  { title: $t('page.mes.importByStatus.total'), key: 'totalRows', width: 90 },
  { title: $t('page.mes.importByStatus.success'), key: 'successRows', width: 90 },
  { title: $t('page.mes.importByStatus.fail'), key: 'failRows', width: 90 },
  { title: $t('page.mes.importByStatus.startedAt'), key: 'startedAt', width: 180 }
]);

async function fetchData() {
  loading.value = true;
  try {
    const res = await getImportTasksByStatus(status.value, importType.value.trim() || undefined);
    if (res.error) {
      rows.value = [];
      return;
    }
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
          <span>{{ $t('page.mes.importByStatus.title') }}</span>
          <div class="flex items-center gap-8px">
            <NSelect v-model:value="status" class="w-180px" :options="statusOptions" />
            <NInput v-model:value="importType" class="w-180px" :placeholder="$t('page.mes.importByStatus.type')" @keyup.enter="fetchData" />
            <NButton type="primary" :loading="loading" @click="fetchData">{{ $t('common.search') }}</NButton>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.importByStatus.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
  </div>
</template>
