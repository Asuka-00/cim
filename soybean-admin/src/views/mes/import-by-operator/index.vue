<script setup lang="ts">
import { computed, ref } from 'vue';
import type { DataTableColumns } from 'naive-ui';
import { getImportTasksByOperator, type ImportTask } from '@/service/api/mes/import-task';
import { $t } from '@/locales';

defineOptions({ name: 'MesImportByOperator' });

const STATUSES = ['DRAFT', 'PENDING', 'RUNNING', 'SUCCESS', 'PARTIAL', 'FAILED', 'CANCELLED'];
const operator = ref('');
const status = ref<string | null>(null);
const loading = ref(false);
const rows = ref<ImportTask[]>([]);
const statusOptions = STATUSES.map(item => ({ label: item, value: item }));
const columns = computed<DataTableColumns<ImportTask>>(() => [
  { title: $t('page.mes.importByOperator.taskNo'), key: 'taskNo', width: 160 },
  { title: $t('page.mes.importByOperator.type'), key: 'importType', width: 140 },
  { title: $t('page.mes.importByOperator.status'), key: 'status', width: 120 },
  { title: $t('page.mes.importByOperator.total'), key: 'totalRows', width: 90 },
  { title: $t('page.mes.importByOperator.success'), key: 'successRows', width: 90 },
  { title: $t('page.mes.importByOperator.fail'), key: 'failRows', width: 90 },
  { title: $t('page.mes.importByOperator.startedAt'), key: 'startedAt', width: 180 }
]);

async function fetchData() {
  const name = operator.value.trim();
  if (!name) {
    window.$message?.warning($t('page.mes.importByOperator.required'));
    rows.value = [];
    return;
  }
  loading.value = true;
  try {
    const res = await getImportTasksByOperator(name, status.value || undefined);
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
          <span>{{ $t('page.mes.importByOperator.title') }}</span>
          <div class="flex items-center gap-8px">
            <NInput v-model:value="operator" class="w-180px" :placeholder="$t('page.mes.importByOperator.operator')" @keyup.enter="fetchData" />
            <NSelect v-model:value="status" class="w-180px" :options="statusOptions" clearable />
            <NButton type="primary" :loading="loading" @click="fetchData">{{ $t('common.search') }}</NButton>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.importByOperator.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
  </div>
</template>
