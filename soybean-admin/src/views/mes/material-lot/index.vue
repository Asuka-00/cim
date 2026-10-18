<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import type { DataTableColumns } from 'naive-ui';
import { getMaterialLots, type MaterialLot } from '@/service/api/mes/inspection';
import { getMaterialList, type Material } from '@/service/api/mes/material';
import { getUomList, type Uom } from '@/service/api/mes/uom';
import { $t } from '@/locales';

defineOptions({ name: 'MesMaterialLot' });

const loading = ref(false);
const rows = ref<MaterialLot[]>([]);
const materials = ref<Material[]>([]);
const uoms = ref<Uom[]>([]);

function materialLabel(id?: string | null) {
  const material = materials.value.find(item => String(item.id) === String(id));
  return material ? `${material.materialCode} ${material.materialName}` : String(id ?? '');
}

function uomLabel(id?: string | null) {
  if (!id) return '';
  const uom = uoms.value.find(item => String(item.id) === String(id));
  return uom ? uom.uomCode : String(id);
}

const columns = computed<DataTableColumns<MaterialLot>>(() => [
  { title: $t('page.mes.materialLot.code'), key: 'lotCode', width: 160 },
  { title: $t('page.mes.materialLot.material'), key: 'materialId', render: row => materialLabel(row.materialId) },
  { title: $t('page.mes.materialLot.qty'), key: 'receivedQty', width: 110 },
  { title: $t('page.mes.materialLot.uom'), key: 'uomId', width: 100, render: row => uomLabel(row.uomId) },
  { title: $t('page.mes.materialLot.status'), key: 'status', width: 130 },
  { title: $t('page.mes.materialLot.qc'), key: 'qcResult', width: 120 },
  { title: $t('page.mes.materialLot.expire'), key: 'expireDate', width: 130 },
  { title: $t('page.mes.materialLot.receivedAt'), key: 'receivedAt', width: 180 }
]);

onMounted(async () => {
  loading.value = true;
  try {
    const [lotRes, materialPage, uomPage] = await Promise.all([
      getMaterialLots(),
      getMaterialList({ page: 1, pageSize: 200 }),
      getUomList({ page: 1, pageSize: 200 })
    ]);
    rows.value = lotRes.data ?? [];
    materials.value = materialPage.data?.records ?? [];
    uoms.value = uomPage.data?.records ?? [];
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>{{ $t('page.mes.materialLot.title') }}</template>
      <div class="mb-12px">{{ $t('page.mes.materialLot.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
  </div>
</template>
