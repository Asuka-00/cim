<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import type { DataTableColumns } from 'naive-ui';
import { getEquipmentList, getEquipmentStatusHistory, type Equipment, type EquipmentStatusHist } from '@/service/api/mes/equipment';
import { getEquipmentPorts, type EquipmentPort } from '@/service/api/mes/equipment-port';
import { $t } from '@/locales';

defineOptions({ name: 'MesStatusHist' });

const equipment = ref<Equipment[]>([]);
const ports = ref<EquipmentPort[]>([]);
const equipmentId = ref<string | null>(null);
const loading = ref(false);
const rows = ref<EquipmentStatusHist[]>([]);
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);

const equipmentOptions = computed(() =>
  equipment.value.map(item => ({ label: `${item.equipmentCode} ${item.equipmentName}`, value: String(item.id) }))
);

function portLabel(id?: string | null) {
  if (!id || id === '0') return $t('page.mes.statusHist.equipmentLevel');
  const port = ports.value.find(item => String(item.id) === String(id));
  return port ? port.portCode : String(id);
}

const columns = computed<DataTableColumns<EquipmentStatusHist>>(() => [
  { title: $t('page.mes.statusHist.port'), key: 'portId', width: 140, render: row => portLabel(row.portId) },
  { title: $t('page.mes.equipment.fromState'), key: 'fromE10State', width: 140 },
  { title: $t('page.mes.equipment.toState'), key: 'toE10State', width: 160 },
  { title: $t('page.mes.equipment.reasonCode'), key: 'reasonCode', width: 120 },
  { title: $t('page.mes.equipment.startAt'), key: 'startAt', width: 180 },
  { title: $t('page.mes.equipment.endAt'), key: 'endAt', width: 180 },
  { title: $t('page.mes.equipment.durationSec'), key: 'durationSec', width: 90 },
  { title: $t('page.mes.statusHist.source'), key: 'source', width: 110 }
]);

async function fetchData() {
  if (!equipmentId.value) {
    rows.value = [];
    total.value = 0;
    ports.value = [];
    return;
  }
  loading.value = true;
  try {
    const [hist, portRes] = await Promise.all([
      getEquipmentStatusHistory(equipmentId.value, { page: page.value, pageSize: pageSize.value }),
      getEquipmentPorts(equipmentId.value)
    ]);
    rows.value = hist.data?.records ?? [];
    total.value = hist.data?.total ?? 0;
    ports.value = portRes.data ?? [];
  } finally {
    loading.value = false;
  }
}

function onEquipmentChange() {
  page.value = 1;
  fetchData();
}

function onPageUpdate(value: number) {
  page.value = value;
  fetchData();
}

function onSizeUpdate(value: number) {
  pageSize.value = value;
  page.value = 1;
  fetchData();
}

onMounted(async () => {
  const res = await getEquipmentList({ page: 1, pageSize: 200 });
  equipment.value = res.data?.records ?? [];
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.statusHist.title') }}</span>
          <NSelect v-model:value="equipmentId" class="w-280px" :options="equipmentOptions" filterable @update:value="onEquipmentChange" />
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.statusHist.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
      <div class="mt-12px flex justify-end">
        <NPagination
          :page="page"
          :page-size="pageSize"
          :item-count="total"
          show-size-picker
          @update:page="onPageUpdate"
          @update:page-size="onSizeUpdate"
        />
      </div>
    </NCard>
  </div>
</template>
