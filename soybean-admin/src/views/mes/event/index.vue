<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue';
import { NButton, type DataTableColumns } from 'naive-ui';
import { getDeliveryPage, getEventPage, retryDelivery, skipDelivery, type DomainEvent, type EventDelivery } from '@/service/api/mes/event';
import CrudDialog from '@/components/business/crud-dialog.vue';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import { useAuth } from '@/hooks/business/auth';
import { usePageQuery } from '@/hooks/business/page-query';
import { $t } from '@/locales';

defineOptions({ name: 'MesEvent' });

const { hasAuth } = useAuth();
const events = usePageQuery<DomainEvent, { status: string }, { status?: string }>({
  initialQuery: { status: '' },
  fetcher: getEventPage,
  buildQuery: query => (query.status ? { status: query.status } : {})
});
const deliveries = usePageQuery<EventDelivery, { subscriber: string; status: string }, { subscriber?: string; status?: string }>({
  initialQuery: { subscriber: '', status: '' },
  fetcher: getDeliveryPage,
  buildQuery: query => {
    const params: { subscriber?: string; status?: string } = {};
    if (query.subscriber) params.subscriber = query.subscriber;
    if (query.status) params.status = query.status;
    return params;
  }
});

const eventFields = computed<FilterField[]>(() => [{ key: 'status', label: $t('page.mes.event.status'), type: 'input' }]);
const deliveryFields = computed<FilterField[]>(() => [
  { key: 'subscriber', label: $t('page.mes.event.subscriber'), type: 'input' },
  { key: 'status', label: $t('page.mes.event.status'), type: 'input' }
]);
const showPayload = ref(false);
const payload = ref('');

const eventColumns = computed<DataTableColumns<DomainEvent>>(() => [
  { title: $t('page.mes.event.type'), key: 'eventType', width: 180 },
  { title: $t('page.mes.event.aggregate'), key: 'aggregateType', width: 140 },
  { title: $t('page.mes.event.status'), key: 'status', width: 120 },
  { title: $t('page.mes.event.retryCount'), key: 'retryCount', width: 90 },
  { title: $t('page.mes.event.error'), key: 'errorMessage' },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 90,
    render: row => h(NButton, { size: 'small', quaternary: true, onClick: () => openPayload(row) }, () => $t('page.mes.event.payload'))
  }
]);

const deliveryColumns = computed<DataTableColumns<EventDelivery>>(() => [
  { title: $t('page.mes.event.subscriber'), key: 'subscriber', width: 160 },
  { title: $t('page.mes.event.aggregate'), key: 'aggregateType', width: 140 },
  { title: $t('page.mes.event.status'), key: 'status', width: 110 },
  { title: $t('page.mes.event.retryCount'), key: 'retryCount', width: 90 },
  { title: $t('page.mes.event.error'), key: 'errorMessage' },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 150,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        hasAuth('mes:event:retry')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => retry(row) }, () => $t('page.mes.event.retry'))
          : null,
        hasAuth('mes:event:skip')
          ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => skip(row) }, () => $t('page.mes.event.skip'))
          : null
      ])
  }
]);

function openPayload(row: DomainEvent) {
  payload.value = row.payloadJson ?? '';
  showPayload.value = true;
}

async function retry(row: EventDelivery) {
  await retryDelivery(String(row.id));
  window.$message?.success($t('common.saveSuccess'));
  await deliveries.fetchData();
}

async function skip(row: EventDelivery) {
  await skipDelivery(String(row.id));
  window.$message?.success($t('common.saveSuccess'));
  await deliveries.fetchData();
}

onMounted(() => {
  events.fetchData();
  deliveries.fetchData();
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>{{ $t('page.mes.event.title') }}</template>
      <div class="mb-12px">{{ $t('page.mes.event.hint') }}</div>
      <div class="mb-8px font-medium">{{ $t('page.mes.event.events') }}</div>
      <FilterBar v-model="events.searchParams" :fields="eventFields" @search="events.handleSearch" @reset="events.handleReset" />
      <DataTable :columns="eventColumns" :data="events.list.value" :loading="events.loading.value" :bordered="false" size="small" />
      <div class="mb-16px mt-12px flex justify-end">
        <NPagination
          :page="events.currentPage.value"
          :page-size="events.pageSize.value"
          :item-count="events.total.value"
          @update:page="events.onPageUpdate"
          @update:page-size="events.onSizeUpdate"
        />
      </div>
      <div class="mb-8px font-medium">{{ $t('page.mes.event.deliveries') }}</div>
      <FilterBar v-model="deliveries.searchParams" :fields="deliveryFields" @search="deliveries.handleSearch" @reset="deliveries.handleReset" />
      <DataTable :columns="deliveryColumns" :data="deliveries.list.value" :loading="deliveries.loading.value" :bordered="false" size="small" />
      <div class="mt-12px flex justify-end">
        <NPagination
          :page="deliveries.currentPage.value"
          :page-size="deliveries.pageSize.value"
          :item-count="deliveries.total.value"
          @update:page="deliveries.onPageUpdate"
          @update:page-size="deliveries.onSizeUpdate"
        />
      </div>
    </NCard>
    <CrudDialog v-model:show="showPayload" :title="$t('page.mes.event.payload')" class="w-720px" hide-footer>
      <NInput :value="payload" type="textarea" :rows="12" readonly />
    </CrudDialog>
  </div>
</template>
