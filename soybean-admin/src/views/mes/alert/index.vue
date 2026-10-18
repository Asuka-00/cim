<script setup lang="ts">
import { computed, h, onMounted } from 'vue';
import { NButton, type DataTableColumns } from 'naive-ui';
import { closeAlert, getAlertPage, readAlert, type MesAlert } from '@/service/api/mes/alert';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesAlert' });

const { hasAuth } = useAuth();
const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<MesAlert, { status: string }>({
    initialQuery: { status: '' },
    fetcher: getAlertPage
  });

const filterFields = computed<FilterField[]>(() => [
  {
    key: 'status',
    label: $t('page.mes.alert.status'),
    type: 'select',
    options: [
      { label: $t('page.mes.alert.all'), value: '' },
      { label: 'NEW', value: 'NEW' },
      { label: 'READ', value: 'READ' },
      { label: 'CLOSED', value: 'CLOSED' }
    ]
  }
]);

const columns = computed<DataTableColumns<MesAlert>>(() => [
  { title: $t('page.mes.alert.type'), key: 'alertType', width: 150 },
  { title: $t('page.mes.alert.level'), key: 'level', width: 90 },
  { title: $t('page.mes.alert.titleText'), key: 'title' },
  { title: $t('page.mes.alert.status'), key: 'status', width: 100 },
  { title: $t('page.mes.alert.content'), key: 'content' },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 140,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        row.status === 'NEW' && hasAuth('mes:alert:read')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => markRead(row) }, () => $t('page.mes.alert.read'))
          : null,
        row.status !== 'CLOSED' && hasAuth('mes:alert:close')
          ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => markClose(row) }, () => $t('page.mes.alert.close'))
          : null
      ])
  }
]);

async function markRead(row: MesAlert) {
  await readAlert(String(row.id));
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

function markClose(row: MesAlert) {
  window.$dialog?.warning({
    title: $t('page.mes.alert.close'),
    content: $t('page.mes.alert.closeConfirm', { name: row.title }),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await closeAlert(String(row.id));
      window.$message?.success($t('common.saveSuccess'));
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
        <span>{{ $t('page.mes.alert.pageTitle') }}</span>
      </template>
      <div class="mb-12px">{{ $t('page.mes.alert.hint') }}</div>
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
  </div>
</template>
