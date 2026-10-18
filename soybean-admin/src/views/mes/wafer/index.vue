<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue';
import { NButton, type DataTableColumns } from 'naive-ui';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { getLotWafers, type LotWafer } from '@/service/api/mes/split';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesWafer' });

const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string; status: string }>({
    initialQuery: { lotNo: '', status: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'lotNo', label: $t('page.mes.wafer.lotNo'), type: 'input' }
]);

const showModal = ref(false);
const waferLoading = ref(false);
const wafers = ref<LotWafer[]>([]);

const waferColumns = computed<DataTableColumns<LotWafer>>(() => [
  { title: $t('page.mes.wafer.slotNo'), key: 'slotNo', width: 90 },
  { title: $t('page.mes.wafer.waferCode'), key: 'waferCode' },
  { title: $t('page.mes.wafer.status'), key: 'status', width: 120 }
]);

async function openWafers(row: Lot) {
  wafers.value = [];
  showModal.value = true;
  waferLoading.value = true;
  try {
    const res = await getLotWafers(String(row.id));
    wafers.value = res.data ?? [];
  } finally {
    waferLoading.value = false;
  }
}

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.wafer.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.wafer.lotStatus'), key: 'status', width: 120 },
  { title: $t('page.mes.wafer.qty'), key: 'qty', width: 100 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 100,
    render: row =>
      hasAuth('mes:lot:wafers')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openWafers(row) }, () =>
            $t('page.mes.wafer.open')
          )
        : null
  }
]);

onMounted(fetchData);
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
        <span>{{ $t('page.mes.wafer.title') }}</span>
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
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.wafer.openTitle')" class="w-640px" hide-footer>
      <DataTable :columns="waferColumns" :data="wafers" :loading="waferLoading" size="small" />
    </CrudDialog>
  </div>
</template>
