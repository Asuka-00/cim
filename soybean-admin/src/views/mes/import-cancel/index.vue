<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue';
import { NButton, type DataTableColumns } from 'naive-ui';
import { cancelImportTask, getImportTasksByStatus, type ImportTask } from '@/service/api/mes/import-task';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesImportCancel' });

const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<ImportTask[]>([]);
const columns = computed<DataTableColumns<ImportTask>>(() => [
  { title: $t('page.mes.importCancel.taskNo'), key: 'taskNo', width: 180 },
  { title: $t('page.mes.importCancel.type'), key: 'importType', width: 160 },
  { title: $t('page.mes.importCancel.operator'), key: 'operator' },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 100,
    render: row =>
      hasAuth('mes:import:cancel')
        ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => cancel(row) }, () => $t('page.mes.importCancel.cancel'))
        : null
  }
]);

async function fetchData() {
  loading.value = true;
  try {
    const res = await getImportTasksByStatus('DRAFT');
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

function cancel(row: ImportTask) {
  window.$dialog?.warning({
    title: $t('page.mes.importCancel.cancel'),
    content: $t('page.mes.importCancel.confirm', { name: row.taskNo }),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await cancelImportTask(String(row.id));
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
          <span>{{ $t('page.mes.importCancel.title') }}</span>
          <NButton @click="fetchData">{{ $t('page.mes.importCancel.reload') }}</NButton>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.importCancel.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
  </div>
</template>
