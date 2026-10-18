<script setup lang="ts">
import { computed, onMounted } from 'vue';
import type { DataTableColumns } from 'naive-ui';
import { getWaferPage, type WaferRow } from '@/service/api/mes/wafer-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import { usePageQuery } from '@/hooks/business/page-query';
import { $t } from '@/locales';

defineOptions({ name: 'MesWaferQuery' });

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<WaferRow, { waferCode: string; lotId: string }, { waferCode?: string; lotId?: string }>({
    initialQuery: { waferCode: '', lotId: '' },
    fetcher: getWaferPage,
    buildQuery: query => {
      const params: { waferCode?: string; lotId?: string } = {};
      if (query.waferCode) params.waferCode = query.waferCode;
      if (query.lotId) params.lotId = query.lotId;
      return params;
    }
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'waferCode', label: $t('page.mes.waferQuery.code'), type: 'input' },
  { key: 'lotId', label: $t('page.mes.waferQuery.lotId'), type: 'input' }
]);
const columns = computed<DataTableColumns<WaferRow>>(() => [
  { title: $t('page.mes.waferQuery.code'), key: 'waferCode', width: 180 },
  { title: $t('page.mes.waferQuery.slot'), key: 'slotNo', width: 90 },
  { title: $t('page.mes.waferQuery.status'), key: 'status', width: 120 },
  { title: $t('page.mes.waferQuery.purpose'), key: 'purpose', width: 120 },
  { title: $t('page.mes.waferQuery.vendor'), key: 'vendorWaferId' },
  { title: $t('page.mes.waferQuery.scrap'), key: 'scrapReason', width: 140 }
]);

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>{{ $t('page.mes.waferQuery.title') }}</template>
      <div class="mb-12px">{{ $t('page.mes.waferQuery.hint') }}</div>
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
  </div>
</template>
