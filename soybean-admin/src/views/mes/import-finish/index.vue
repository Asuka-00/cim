<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue';
import { NButton, type DataTableColumns } from 'naive-ui';
import { finishImportTask, getRunningImportTasks, type ImportTask } from '@/service/api/mes/import-task';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesImportFinish' });

const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<ImportTask[]>([]);
const columns = computed<DataTableColumns<ImportTask>>(() => [
  { title: $t('page.mes.importFinish.taskNo'), key: 'taskNo', width: 180 },
  { title: $t('page.mes.importFinish.type'), key: 'importType', width: 140 },
  { title: $t('page.mes.importFinish.success'), key: 'successRows', width: 90 },
  { title: $t('page.mes.importFinish.fail'), key: 'failRows', width: 90 },
  { title: $t('page.mes.importFinish.total'), key: 'totalRows', width: 90 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 100,
    render: row =>
      hasAuth('mes:import:finish')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => finish(row) }, () => $t('page.mes.importFinish.finish'))
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

function finish(row: ImportTask) {
  window.$dialog?.warning({
    title: $t('page.mes.importFinish.finish'),
    content: $t('page.mes.importFinish.confirm', { name: row.taskNo }),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await finishImportTask(String(row.id));
      window.$message?.success($t('common.saveSuccess'));
      await fetchData();
    }
  });
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.importFinish.title') }}</span>
          <NButton @click="fetchData">{{ $t('page.mes.importFinish.reload') }}</NButton>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.importFinish.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
  </div>
</template>
