<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { type DataTableColumns } from 'naive-ui';
import { getWipByStep, type WipStep } from '@/service/api/mes/wip-by-step';
import { getStepList, type Step } from '@/service/api/mes/step';
import { $t } from '@/locales';

defineOptions({ name: 'MesWipByStep' });

const loading = ref(false);
const rows = ref<WipStep[]>([]);
const steps = ref<Step[]>([]);

const stepName = computed(() => {
  const names = new Map(steps.value.map(item => [String(item.id), `${item.stepCode} ${item.stepName}`]));
  return (id?: string | null) => (id ? (names.get(String(id)) ?? id) : $t('page.mes.wip.noStep'));
});

const columns = computed<DataTableColumns<WipStep>>(() => [
  { title: $t('page.mes.wip.step'), key: 'stepId', render: row => stepName.value(row.stepId) },
  { title: $t('page.mes.wip.lotCount'), key: 'lotCount', width: 120 },
  { title: $t('page.mes.wip.qty'), key: 'qty', width: 120 },
  { title: $t('page.mes.wip.holdCount'), key: 'holdCount', width: 140 }
]);

async function fetchData() {
  loading.value = true;
  try {
    const [wipRes, stepPage] = await Promise.all([getWipByStep(), getStepList({ page: 1, pageSize: 200 })]);
    rows.value = wipRes.data ?? [];
    steps.value = stepPage.data?.records ?? [];
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
        <span>{{ $t('page.mes.wip.title') }}</span>
      </template>
      <div class="mb-12px">{{ $t('page.mes.wip.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
  </div>
</template>
