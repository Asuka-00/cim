<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue';
import { NButton, type DataTableColumns } from 'naive-ui';
import { getTxnPage, type MesTxn } from '@/service/api/mes/txn';
import CrudDialog from '@/components/business/crud-dialog.vue';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import { usePageQuery } from '@/hooks/business/page-query';
import { $t } from '@/locales';

defineOptions({ name: 'MesTxn' });

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<MesTxn, { txnType: string; objectType: string; objectId: string }, { txnType?: string; objectType?: string; objectId?: string }>({
    initialQuery: { txnType: '', objectType: '', objectId: '' },
    fetcher: getTxnPage,
    buildQuery: query => {
      const params: { txnType?: string; objectType?: string; objectId?: string } = {};
      if (query.txnType) params.txnType = query.txnType;
      if (query.objectType) params.objectType = query.objectType;
      if (query.objectId) params.objectId = query.objectId;
      return params;
    }
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'txnType', label: $t('page.mes.txn.type'), type: 'input' },
  { key: 'objectType', label: $t('page.mes.txn.objectType'), type: 'input' },
  { key: 'objectId', label: $t('page.mes.txn.objectId'), type: 'input' }
]);
const showDetail = ref(false);
const current = ref<MesTxn | null>(null);

const columns = computed<DataTableColumns<MesTxn>>(() => [
  { title: $t('page.mes.txn.time'), key: 'txnTime', width: 180 },
  { title: $t('page.mes.txn.type'), key: 'txnType', width: 140 },
  { title: $t('page.mes.txn.object'), key: 'objectCode' },
  { title: $t('page.mes.txn.result'), key: 'result', width: 100 },
  { title: $t('page.mes.txn.operator'), key: 'operator', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 90,
    render: row => h(NButton, { size: 'small', quaternary: true, onClick: () => openDetail(row) }, () => $t('page.mes.txn.detail'))
  }
]);

function openDetail(row: MesTxn) {
  current.value = row;
  showDetail.value = true;
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>{{ $t('page.mes.txn.title') }}</template>
      <div class="mb-12px">{{ $t('page.mes.txn.hint') }}</div>
      <FilterBar v-model="searchParams" :fields="filterFields" @search="handleSearch" @reset="handleReset" />
      <DataTable flex-height :columns="columns" :data="list" :loading="loading" :bordered="false" size="small" />
      <div class="mt-12px flex justify-end">
        <NPagination
          :page="currentPage"
          :page-size="pageSize"
          :item-count="total"
          show-size-picker
          @update:page="onPageUpdate"
          @update:page-size="onSizeUpdate"
        />
      </div>
    </NCard>
    <CrudDialog v-model:show="showDetail" :title="$t('page.mes.txn.detail')" class="w-720px" hide-footer>
      <div class="mb-8px">{{ current?.errorMessage }}</div>
      <NInput :value="current?.payloadJson ?? ''" type="textarea" :rows="12" readonly />
    </CrudDialog>
  </div>
</template>
