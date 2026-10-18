<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue';
import { NButton, type DataTableColumns } from 'naive-ui';
import { getLotConsumptions, type LotConsumption } from '@/service/api/mes/consumption';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { getMaterialList, type Material } from '@/service/api/mes/material';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesConsumption' });

const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string; status: string }>({
    initialQuery: { lotNo: '', status: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'lotNo', label: $t('page.mes.consumption.lotNo'), type: 'input' }
]);

const showModal = ref(false);
const detailLoading = ref(false);
const rows = ref<LotConsumption[]>([]);
const materials = ref<Material[]>([]);

const materialName = computed(() => {
  const names = new Map(materials.value.map(item => [String(item.id), `${item.materialCode} ${item.materialName}`]));
  return (id: string) => names.get(String(id)) ?? id;
});

const detailColumns = computed<DataTableColumns<LotConsumption>>(() => [
  { title: $t('page.mes.consumption.material'), key: 'materialId', render: row => materialName.value(row.materialId) },
  { title: $t('page.mes.consumption.qty'), key: 'qty', width: 100 },
  { title: $t('page.mes.consumption.source'), key: 'source', width: 120 },
  {
    title: $t('page.mes.consumption.shortage'),
    key: 'shortage',
    width: 80,
    render: row => (row.shortage ? $t('page.mes.consumption.yes') : '')
  }
]);

async function openConsumption(row: Lot) {
  rows.value = [];
  showModal.value = true;
  detailLoading.value = true;
  try {
    const res = await getLotConsumptions(String(row.id));
    rows.value = res.data ?? [];
  } finally {
    detailLoading.value = false;
  }
}

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.consumption.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.consumption.status'), key: 'status', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 100,
    render: row =>
      hasAuth('mes:lot:consumption')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openConsumption(row) }, () =>
            $t('page.mes.consumption.open')
          )
        : null
  }
]);

onMounted(async () => {
  const page = await getMaterialList({ page: 1, pageSize: 200 });
  materials.value = page.data?.records ?? [];
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
        <span>{{ $t('page.mes.consumption.title') }}</span>
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
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.consumption.openTitle')" class="w-720px" hide-footer>
      <DataTable :columns="detailColumns" :data="rows" :loading="detailLoading" size="small" />
    </CrudDialog>
  </div>
</template>
