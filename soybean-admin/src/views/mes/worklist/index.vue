<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { type DataTableColumns } from 'naive-ui';
import { getWorklist, type WorklistItem } from '@/service/api/mes/worklist';
import { getAreaList, type Area } from '@/service/api/mes/area';
import { getDispatchRules, type DispatchRule } from '@/service/api/mes/dispatch-rule';
import { getEquipmentList, type Equipment } from '@/service/api/mes/equipment';
import { getStepList, type Step } from '@/service/api/mes/step';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import { $t } from '@/locales';

defineOptions({ name: 'MesWorklist' });

const loading = ref(false);
const rows = ref<WorklistItem[]>([]);
const steps = ref<Step[]>([]);
const areas = ref<Area[]>([]);
const equipment = ref<Equipment[]>([]);
const rules = ref<DispatchRule[]>([]);
const query = reactive({ stepId: null as string | null, areaId: null as string | null, equipmentId: null as string | null, ruleId: null as string | null });

const stepName = computed(() => {
  const names = new Map(steps.value.map(item => [String(item.id), `${item.stepCode} ${item.stepName}`]));
  return (id?: string) => (id ? (names.get(String(id)) ?? id) : '');
});

const fields = computed<FilterField[]>(() => [
  {
    key: 'stepId',
    label: $t('page.mes.worklist.step'),
    type: 'select',
    options: steps.value.map(item => ({ label: `${item.stepCode} ${item.stepName}`, value: String(item.id) }))
  },
  {
    key: 'areaId',
    label: $t('page.mes.worklist.area'),
    type: 'select',
    options: areas.value.map(item => ({ label: `${item.areaCode} ${item.areaName}`, value: String(item.id) }))
  },
  {
    key: 'equipmentId',
    label: $t('page.mes.worklist.equipment'),
    type: 'select',
    options: equipment.value.map(item => ({ label: `${item.equipmentCode} ${item.equipmentName}`, value: String(item.id) }))
  },
  {
    key: 'ruleId',
    label: $t('page.mes.worklist.rule'),
    type: 'select',
    options: rules.value.map(item => ({ label: item.ruleName, value: String(item.id) }))
  }
]);

const columns = computed<DataTableColumns<WorklistItem>>(() => [
  { title: $t('page.mes.worklist.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.worklist.priority'), key: 'priority', width: 90 },
  { title: $t('page.mes.worklist.hot'), key: 'isHot', width: 80, render: row => (row.isHot === 1 ? $t('page.mes.worklist.yes') : '') },
  { title: $t('page.mes.worklist.step'), key: 'stepId', render: row => stepName.value(row.stepId) },
  { title: $t('page.mes.worklist.enterAt'), key: 'stepEnterAt', width: 180 },
  { title: $t('page.mes.worklist.qty'), key: 'qty', width: 90 }
]);

async function fetchData() {
  loading.value = true;
  try {
    const params: { stepId?: string; areaId?: string; equipmentId?: string; ruleId?: string } = {};
    if (query.stepId) params.stepId = query.stepId;
    if (query.areaId) params.areaId = query.areaId;
    if (query.equipmentId) params.equipmentId = query.equipmentId;
    if (query.ruleId) params.ruleId = query.ruleId;
    const res = await getWorklist(params);
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

function reset() {
  Object.assign(query, { stepId: null, areaId: null, equipmentId: null, ruleId: null });
  fetchData();
}

onMounted(async () => {
  const [stepPage, areaPage, equipmentPage, ruleRes] = await Promise.all([
    getStepList({ page: 1, pageSize: 200 }),
    getAreaList({ page: 1, pageSize: 200 }),
    getEquipmentList({ page: 1, pageSize: 200 }),
    getDispatchRules()
  ]);
  steps.value = stepPage.data?.records ?? [];
  areas.value = areaPage.data?.records ?? [];
  equipment.value = equipmentPage.data?.records ?? [];
  rules.value = ruleRes.data ?? [];
  await fetchData();
});
</script>

<template>
  <div class="rtd-page">
    <FilterBar
      :fields="fields"
      :collapsible="false"
      :model-value="query as Record<string, unknown>"
      @update:model-value="v => Object.assign(query, v)"
      @search="fetchData"
      @reset="reset"
    />
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <span>{{ $t('page.mes.worklist.title') }}</span>
      </template>
      <div class="mb-12px">{{ $t('page.mes.worklist.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
  </div>
</template>
