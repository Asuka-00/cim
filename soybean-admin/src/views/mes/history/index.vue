<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue';
import { NButton, type DataTableColumns } from 'naive-ui';
import { getLotHistory, type LotHistory } from '@/service/api/mes/history';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesHistory' });

const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string; status: string }>({
    initialQuery: { lotNo: '', status: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'lotNo', label: $t('page.mes.history.lotNo'), type: 'input' }
]);

const showModal = ref(false);
const historyLoading = ref(false);
const history = ref<LotHistory[]>([]);

const historyColumns = computed<DataTableColumns<LotHistory>>(() => [
  { title: $t('page.mes.history.occurredAt'), key: 'occurredAt', width: 170 },
  { title: $t('page.mes.history.txnType'), key: 'txnType', width: 140 },
  { title: $t('page.mes.history.statusAfter'), key: 'statusAfter', width: 110 },
  { title: $t('page.mes.history.qtyAfter'), key: 'qtyAfter', width: 90 },
  { title: $t('page.mes.history.reason'), key: 'reasonCode' },
  { title: $t('page.mes.history.operator'), key: 'operator', width: 100 }
]);

async function openHistory(row: Lot) {
  history.value = [];
  showModal.value = true;
  historyLoading.value = true;
  try {
    const res = await getLotHistory(String(row.id));
    history.value = res.data ?? [];
  } finally {
    historyLoading.value = false;
  }
}

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.history.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.history.status'), key: 'status', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 100,
    render: row =>
      hasAuth('mes:lot:history')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openHistory(row) }, () =>
            $t('page.mes.history.open')
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
        <span>{{ $t('page.mes.history.title') }}</span>
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
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.history.openTitle')" class="w-860px" hide-footer>
      <DataTable :columns="historyColumns" :data="history" :loading="historyLoading" size="small" />
    </CrudDialog>
  </div>
</template>
