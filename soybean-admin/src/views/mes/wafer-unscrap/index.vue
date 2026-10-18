<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue';
import { NButton, type DataTableColumns } from 'naive-ui';
import { unscrapWafer } from '@/service/api/mes/wafer-unscrap';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { getLotWafers, type LotWafer } from '@/service/api/mes/split';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesWaferUnscrap' });

const { hasAuth } = useAuth();
const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string }>({
    initialQuery: { lotNo: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [{ key: 'lotNo', label: $t('page.mes.unscrap.lotNo'), type: 'input' }]);
const showWafers = ref(false);
const current = ref<Lot | null>(null);
const wafers = ref<LotWafer[]>([]);

async function openWafers(row: Lot) {
  current.value = row;
  const res = await getLotWafers(String(row.id));
  wafers.value = res.data ?? [];
  showWafers.value = true;
}

const lotColumns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.unscrap.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.unscrap.status'), key: 'status', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 90,
    render: row =>
      h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openWafers(row) }, () => $t('page.mes.unscrap.wafers'))
  }
]);

const waferColumns = computed<DataTableColumns<LotWafer>>(() => [
  { title: $t('page.mes.unscrap.wafer'), key: 'waferCode' },
  { title: $t('page.mes.unscrap.status'), key: 'status', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 110,
    render: row =>
      row.status === 'SCRAPPED' && hasAuth('mes:wafer:unscrap')
        ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => handleUnscrap(row) }, () => $t('page.mes.unscrap.unscrap'))
        : null
  }
]);

async function refreshWafers() {
  if (!current.value) return;
  const res = await getLotWafers(String(current.value.id));
  wafers.value = res.data ?? [];
}

function handleUnscrap(row: LotWafer) {
  window.$dialog?.warning({
    title: $t('page.mes.unscrap.unscrap'),
    content: $t('page.mes.unscrap.confirm', { name: row.waferCode }),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await unscrapWafer(String(row.id));
      window.$message?.success($t('common.saveSuccess'));
      await refreshWafers();
      await fetchData();
    }
  });
}

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
        <span>{{ $t('page.mes.unscrap.title') }}</span>
      </template>
      <DataTable flex-height :columns="lotColumns" :data="list" :loading="loading" :bordered="false" size="small" />
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
    <CrudDialog v-model:show="showWafers" :title="$t('page.mes.unscrap.wafers')" class="w-640px" hide-footer>
      <div class="mb-12px">{{ $t('page.mes.unscrap.hint') }}</div>
      <DataTable :columns="waferColumns" :data="wafers" size="small" />
    </CrudDialog>
  </div>
</template>
