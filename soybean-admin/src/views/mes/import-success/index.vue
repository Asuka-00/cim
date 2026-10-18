<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue';
import { NButton, type DataTableColumns } from 'naive-ui';
import { getRunningImportTasks, recordImportSuccess, type ImportTask } from '@/service/api/mes/import-task';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesImportSuccess' });

const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<ImportTask[]>([]);
const columns = computed<DataTableColumns<ImportTask>>(() => [
  { title: $t('page.mes.importSuccess.taskNo'), key: 'taskNo', width: 180 },
  { title: $t('page.mes.importSuccess.type'), key: 'importType', width: 140 },
  { title: $t('page.mes.importSuccess.mode'), key: 'mode', width: 140 },
  { title: $t('page.mes.importSuccess.success'), key: 'successRows', width: 90 },
  { title: $t('page.mes.importSuccess.fail'), key: 'failRows', width: 90 },
  { title: $t('page.mes.importSuccess.total'), key: 'totalRows', width: 90 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 120,
    render: row =>
      hasAuth('mes:import:success')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => record(row) }, () =>
            $t('page.mes.importSuccess.record')
          )
        : null
  }
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

async function record(row: ImportTask) {
  await recordImportSuccess(String(row.id));
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.importSuccess.title') }}</span>
          <NButton @click="fetchData">{{ $t('page.mes.importSuccess.reload') }}</NButton>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.importSuccess.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
  </div>
</template>
