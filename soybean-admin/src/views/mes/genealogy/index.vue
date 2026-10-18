<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue';
import { NButton, type DataTableColumns } from 'naive-ui';
import { getLotChildren, getLotParent, getMergeSources, type LotLink } from '@/service/api/mes/genealogy';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesGenealogy' });

const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string; status: string }>({
    initialQuery: { lotNo: '', status: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'lotNo', label: $t('page.mes.genealogy.lotNo'), type: 'input' }
]);

const showModal = ref(false);
const detailLoading = ref(false);
const parent = ref<LotLink | null>(null);
const children = ref<LotLink[]>([]);
const sources = ref<LotLink[]>([]);

const linkColumns = computed<DataTableColumns<LotLink>>(() => [
  { title: $t('page.mes.genealogy.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.genealogy.qty'), key: 'qty', width: 100 }
]);

async function openGenealogy(row: Lot) {
  parent.value = null;
  children.value = [];
  sources.value = [];
  showModal.value = true;
  detailLoading.value = true;
  try {
    const id = String(row.id);
    const [parentRes, childRes, sourceRes] = await Promise.all([getLotParent(id), getLotChildren(id), getMergeSources(id)]);
    parent.value = parentRes.data ?? null;
    children.value = childRes.data ?? [];
    sources.value = sourceRes.data ?? [];
  } finally {
    detailLoading.value = false;
  }
}

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.genealogy.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.genealogy.status'), key: 'status', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 100,
    render: row =>
      hasAuth('mes:lot:genealogy')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openGenealogy(row) }, () =>
            $t('page.mes.genealogy.open')
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
        <span>{{ $t('page.mes.genealogy.title') }}</span>
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
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.genealogy.openTitle')" class="w-640px" hide-footer>
      <div class="mb-8px">{{ $t('page.mes.genealogy.parent') }}：{{ parent?.lotNo || $t('page.mes.genealogy.none') }}</div>
      <div class="mb-8px">{{ $t('page.mes.genealogy.children') }}</div>
      <DataTable :columns="linkColumns" :data="children" :loading="detailLoading" size="small" class="mb-12px" />
      <div class="mb-8px">{{ $t('page.mes.genealogy.sources') }}</div>
      <DataTable :columns="linkColumns" :data="sources" :loading="detailLoading" size="small" />
    </CrudDialog>
  </div>
</template>
