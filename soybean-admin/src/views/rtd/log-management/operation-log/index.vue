<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue';
import { NButton, type DataTableColumns } from 'naive-ui';
import {
  getOperationLogList,
  type OperationLogQuery,
  type OperationLogVO
} from '@/service/api/rtd/log-management/operation-log';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import { formatJsonPayload } from '@/utils/common';
import { rangeToDayBounds } from '@/utils/datetime';
import { $t } from '@/locales';
import { renderBoolTag } from '@/utils/tag-render';

defineOptions({ name: 'RtdLogManagementOperationLog' });

type SearchParams = OperationLogQuery & { createAtRange?: [number, number] | null };

const {
  loading,
  list,
  total,
  currentPage,
  pageSize,
  searchParams,
  fetchData,
  handleSearch,
  handleReset,
  onPageUpdate,
  onSizeUpdate
} = usePageQuery<OperationLogVO, SearchParams, OperationLogQuery>({
  initialQuery: {
    traceId: '',
    userName: '',
    operation: '',
    className: '',
    methodName: '',
    requestUri: '',
    requestMethod: undefined,
    success: undefined,
    createAtRange: null
  },
  fetcher: getOperationLogList,
  buildQuery: q => {
    const { createAtRange, ...rest } = q;
    const bounds = rangeToDayBounds(createAtRange);
    return {
      ...rest,
      createAtStart: bounds?.start,
      createAtEnd: bounds?.end
    };
  }
});

const filterFields = computed<FilterField[]>(() => [
  { key: 'traceId', label: 'Trace ID', type: 'input' },
  { key: 'userName', label: $t('page.logManagement.operationLog.userName'), type: 'input' },
  { key: 'operation', label: $t('page.logManagement.operationLog.operation'), type: 'input' },
  { key: 'requestUri', label: $t('page.logManagement.operationLog.requestUri'), type: 'input' },
  {
    key: 'requestMethod',
    label: $t('page.logManagement.operationLog.requestMethod'),
    type: 'select',
    options: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'].map(m => ({ label: m, value: m }))
  },
  {
    key: 'success',
    label: $t('page.logManagement.operationLog.isSuccess'),
    type: 'select',
    options: [
      { label: $t('common.success'), value: true },
      { label: $t('common.failed'), value: false }
    ]
  },
  { key: 'createAtRange', label: $t('common.timeRange'), type: 'daterange' }
]);

const showDetail = ref(false);
const detail = ref<OperationLogVO | null>(null);

const columns = computed<DataTableColumns<OperationLogVO>>(() => [
  {
    title: '#',
    key: 'index',
    width: 60,
    render: (_r, i) => (currentPage.value - 1) * pageSize.value + i + 1
  },
  { title: $t('page.logManagement.common.time'), key: 'createAt', width: 170 },
  { title: $t('page.logManagement.operationLog.user'), key: 'userName', width: 100 },
  { title: $t('page.logManagement.operationLog.operation'), key: 'operation', ellipsis: { tooltip: true } },
  { title: $t('page.logManagement.operationLog.method'), key: 'requestMethod', width: 80 },
  { title: 'URI', key: 'requestUri', ellipsis: { tooltip: true } },
  {
    title: $t('page.logManagement.operationLog.classMethod'),
    key: 'className',
    ellipsis: { tooltip: true },
    render: row => `${row.className}.${row.methodName}`
  },
  {
    title: $t('common.success'),
    key: 'success',
    width: 70,
    render: row => renderBoolTag(row.success, 'success', 'error')
  },
  { title: $t('page.logManagement.common.elapsedMs'), key: 'durationMs', width: 90 },
  { title: $t('page.logManagement.operationLog.errorType'), key: 'errorType', width: 120, ellipsis: { tooltip: true } },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 80,
    render: row =>
      h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openDetail(row) }, () =>
        $t('common.detail')
      )
  }
]);

function openDetail(row: OperationLogVO) {
  detail.value = row;
  showDetail.value = true;
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
        <span>{{ $t('page.logManagement.operationLog.title') }}</span>
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

    <NModal
      v-model:show="showDetail"
      preset="card"
      :title="$t('page.logManagement.operationLog.detailTitle', { operation: detail?.operation ?? '' })"
      :style="{ width: '720px', maxWidth: '90vw' }"
    >
      <NDescriptions v-if="detail" :column="1" label-placement="left" bordered size="small">
        <NDescriptionsItem label="Trace ID">{{ detail.traceId }}</NDescriptionsItem>
        <NDescriptionsItem :label="$t('page.logManagement.operationLog.user')">{{ detail.userName }}</NDescriptionsItem>
        <NDescriptionsItem label="IP">{{ detail.requestIp }}</NDescriptionsItem>
        <NDescriptionsItem :label="$t('page.logManagement.operationLog.className')">
          {{ detail.className }}
        </NDescriptionsItem>
        <NDescriptionsItem :label="$t('page.logManagement.operationLog.methodName')">
          {{ detail.methodName }}
        </NDescriptionsItem>
        <NDescriptionsItem :label="$t('page.logManagement.operationLog.errorType')">
          {{ detail.errorType }}
        </NDescriptionsItem>
        <NDescriptionsItem :label="$t('page.logManagement.common.errorMsg')">
          {{ detail.errorMessage }}
        </NDescriptionsItem>
        <NDescriptionsItem label="User-Agent">{{ detail.userAgent }}</NDescriptionsItem>
        <NDescriptionsItem :label="$t('page.logManagement.operationLog.requestPayload')">
          <NCode
            class="json-payload max-h-240px overflow-auto"
            language="json"
            :code="formatJsonPayload(detail.requestPayload)"
            word-wrap
          />
        </NDescriptionsItem>
        <NDescriptionsItem :label="$t('page.logManagement.operationLog.responsePayload')">
          <NCode
            class="json-payload max-h-240px overflow-auto"
            language="json"
            :code="formatJsonPayload(detail.responsePayload)"
            word-wrap
          />
        </NDescriptionsItem>
      </NDescriptions>
    </NModal>
  </div>
</template>

<style scoped>
.json-payload {
  display: block;
  max-width: 100%;
  min-width: 0;
  font-size: 12px;
}
</style>
