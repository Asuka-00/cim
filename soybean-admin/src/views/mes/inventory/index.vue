<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import type { DataTableColumns } from 'naive-ui';
import { getInventories, type InventoryRow } from '@/service/api/mes/inventory';
import { getLocations, type StorageLocation } from '@/service/api/mes/location';
import { getMaterialList, type Material } from '@/service/api/mes/material';
import { $t } from '@/locales';

defineOptions({ name: 'MesInventory' });

const loading = ref(false);
const rows = ref<InventoryRow[]>([]);
const materials = ref<Material[]>([]);
const locations = ref<StorageLocation[]>([]);

const materialName = computed(() => {
  const names = new Map(materials.value.map(item => [String(item.id), `${item.materialCode} ${item.materialName}`]));
  return (id: string) => names.get(String(id)) ?? id;
});
const locationName = computed(() => {
  const names = new Map(locations.value.map(item => [String(item.id), item.locCode]));
  return (id: string) => names.get(String(id)) ?? id;
});

const columns = computed<DataTableColumns<InventoryRow>>(() => [
  { title: $t('page.mes.inventory.material'), key: 'materialId', render: row => materialName.value(row.materialId) },
  { title: $t('page.mes.inventory.location'), key: 'locationId', render: row => locationName.value(row.locationId) },
  { title: $t('page.mes.inventory.qtyOnHand'), key: 'qtyOnHand', width: 120 },
  { title: $t('page.mes.inventory.qtyFrozen'), key: 'qtyFrozen', width: 120 }
]);

onMounted(async () => {
  loading.value = true;
  try {
    const [inventoryRes, materialPage, locationRes] = await Promise.all([
      getInventories(),
      getMaterialList({ page: 1, pageSize: 200 }),
      getLocations()
    ]);
    rows.value = inventoryRes.data ?? [];
    materials.value = materialPage.data?.records ?? [];
    locations.value = locationRes.data ?? [];
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <span>{{ $t('page.mes.inventory.title') }}</span>
      </template>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
  </div>
</template>
