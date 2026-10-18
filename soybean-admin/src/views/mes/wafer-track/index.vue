<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { waferTrackIn, waferTrackOut } from '@/service/api/mes/wafer-track';
import { getEquipmentList, type Equipment } from '@/service/api/mes/equipment';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { getLotWafers, type LotWafer } from '@/service/api/mes/split';
import { getStepList, type Step } from '@/service/api/mes/step';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesWaferTrack' });

const ALIVE = new Set(['NORMAL', 'RUNNING']);
const { hasAuth } = useAuth();
const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string }>({
    initialQuery: { lotNo: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [{ key: 'lotNo', label: $t('page.mes.waferTrack.lotNo'), type: 'input' }]);
const steps = ref<Step[]>([]);
const equipment = ref<Equipment[]>([]);
const wafers = ref<LotWafer[]>([]);
const showIn = ref(false);
const showOut = ref(false);
const inRef = ref<FormInst | null>(null);
const outRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const inForm = reactive({ waferIds: [] as string[], stepId: null as string | null, equipmentId: null as string | null });
const outForm = reactive({ waferIds: [] as string[] });

const inRules = computed<FormRules>(() => ({
  waferIds: [{ required: true, type: 'array', message: $t('page.mes.waferTrack.required'), trigger: 'change' }],
  stepId: [{ required: true, message: $t('page.mes.waferTrack.required'), trigger: 'change' }]
}));
const outRules = computed<FormRules>(() => ({
  waferIds: [{ required: true, type: 'array', message: $t('page.mes.waferTrack.required'), trigger: 'change' }]
}));

const waferOptions = computed(() =>
  wafers.value.filter(item => ALIVE.has(item.status ?? '')).map(item => ({ label: item.waferCode, value: String(item.id) }))
);
const stepOptions = computed(() => steps.value.map(item => ({ label: `${item.stepCode} ${item.stepName}`, value: String(item.id) })));
const equipmentOptions = computed(() =>
  equipment.value.map(item => ({ label: `${item.equipmentCode} ${item.equipmentName}`, value: String(item.id) }))
);

async function loadWafers(row: Lot) {
  current.value = row;
  const res = await getLotWafers(String(row.id));
  wafers.value = res.data ?? [];
}

async function openIn(row: Lot) {
  await loadWafers(row);
  Object.assign(inForm, { waferIds: [], stepId: row.stepId ? String(row.stepId) : null, equipmentId: null });
  showIn.value = true;
}

async function openOut(row: Lot) {
  await loadWafers(row);
  outForm.waferIds = [];
  showOut.value = true;
}

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.waferTrack.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.waferTrack.status'), key: 'status', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 160,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        row.status === 'WAITING' && hasAuth('mes:wafer-track:in')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openIn(row) }, () => $t('page.mes.waferTrack.trackIn'))
          : null,
        row.status === 'RUNNING' && hasAuth('mes:wafer-track:out')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openOut(row) }, () => $t('page.mes.waferTrack.trackOut'))
          : null
      ])
  }
]);

async function submitIn() {
  await inRef.value?.validate();
  if (!current.value || !inForm.stepId) return;
  await waferTrackIn(String(current.value.id), {
    waferIds: inForm.waferIds,
    stepId: inForm.stepId,
    equipmentId: inForm.equipmentId
  });
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

async function submitOut() {
  await outRef.value?.validate();
  if (!current.value) return;
  await waferTrackOut(String(current.value.id), { waferIds: outForm.waferIds });
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

onMounted(async () => {
  const [stepPage, equipmentPage] = await Promise.all([
    getStepList({ page: 1, pageSize: 200 }),
    getEquipmentList({ page: 1, pageSize: 200 })
  ]);
  steps.value = stepPage.data?.records ?? [];
  equipment.value = equipmentPage.data?.records ?? [];
  await fetchData();
});
</script>

<template>
  <div class="rtd-page">
    <FilterBar
      :fields="filterFields"
      :model-value="searchParams as Record<string, unknown>"
      @update:model-value="v => Object.assign(searchParams, v)"
      @search="handleSearch"
      @reset="handleReset"
    />
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <span>{{ $t('page.mes.waferTrack.title') }}</span>
      </template>
      <DataTable flex-height :columns="columns" :data="list" :loading="loading" :bordered="false" size="small" />
      <div class="mt-12px flex justify-end">
        <NPagination
          :page="currentPage"
          :page-size="pageSize"
          :item-count="total"
          show-size-picker
          :page-sizes="[10, 20, 50]"
          @update:page="onPageUpdate"
          @update:page-size="onSizeUpdate"
        />
      </div>
    </NCard>
    <CrudDialog v-model:show="showIn" :title="$t('page.mes.waferTrack.trackInTitle')" class="w-640px" :on-confirm="submitIn">
      <div class="mb-12px">{{ $t('page.mes.waferTrack.hint') }}</div>
      <NForm ref="inRef" :model="inForm" :rules="inRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.waferTrack.wafers')" path="waferIds">
          <NSelect v-model:value="inForm.waferIds" :options="waferOptions" multiple filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.waferTrack.step')" path="stepId">
          <NSelect v-model:value="inForm.stepId" :options="stepOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.waferTrack.equipment')" path="equipmentId">
          <NSelect v-model:value="inForm.equipmentId" :options="equipmentOptions" clearable filterable />
        </NFormItem>
      </NForm>
    </CrudDialog>
    <CrudDialog v-model:show="showOut" :title="$t('page.mes.waferTrack.trackOutTitle')" class="w-640px" :on-confirm="submitOut">
      <div class="mb-12px">{{ $t('page.mes.waferTrack.hint') }}</div>
      <NForm ref="outRef" :model="outForm" :rules="outRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.waferTrack.wafers')" path="waferIds">
          <NSelect v-model:value="outForm.waferIds" :options="waferOptions" multiple filterable />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
