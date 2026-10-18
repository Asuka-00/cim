<script setup lang="ts">
import { ref } from 'vue';
import { getImportTask, type ImportTask } from '@/service/api/mes/import-task';
import { $t } from '@/locales';

defineOptions({ name: 'MesImportByNo' });

const taskNo = ref('');
const loading = ref(false);
const current = ref<ImportTask | null>(null);

async function fetchData() {
  const no = taskNo.value.trim();
  if (!no) {
    window.$message?.warning($t('page.mes.importByNo.required'));
    current.value = null;
    return;
  }
  loading.value = true;
  current.value = null;
  try {
    const res = await getImportTask(no);
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
          <span>{{ $t('page.mes.importByNo.title') }}</span>
          <div class="flex items-center gap-8px">
            <NInput v-model:value="taskNo" class="w-240px" :placeholder="$t('page.mes.importByNo.taskNo')" @keyup.enter="fetchData" />
            <NButton type="primary" :loading="loading" @click="fetchData">{{ $t('common.search') }}</NButton>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.importByNo.hint') }}</div>
      <div v-if="current" class="flex flex-col gap-8px">
        <div>{{ $t('page.mes.importByNo.taskNo') }}：{{ current.taskNo }}</div>
        <div>{{ $t('page.mes.importByNo.type') }}：{{ current.importType }}</div>
        <div>{{ $t('page.mes.importByNo.status') }}：{{ current.status }}</div>
        <div>{{ $t('page.mes.importByNo.operator') }}：{{ current.operator }}</div>
        <div>{{ $t('page.mes.importByNo.rows') }}：{{ current.successRows ?? 0 }} / {{ current.failRows ?? 0 }} / {{ current.totalRows ?? 0 }}</div>
        <div>{{ $t('page.mes.importByNo.startedAt') }}：{{ current.startedAt }}</div>
      </div>
    </NCard>
  </div>
</template>
