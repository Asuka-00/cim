<script setup lang="ts">
import { computed, ref } from 'vue';
import type { DataTableColumns } from 'naive-ui';
import { getApprovalRecords, type ApprovalRecord } from '@/service/api/mes/approval-record';
import { $t } from '@/locales';

defineOptions({ name: 'MesApprovalRecord' });

const TYPES = ['PRODUCT_VERSION', 'ROUTE_VERSION', 'BOM_VERSION', 'RECIPE_VERSION', 'CONSUMPTION_ADJUST', 'ECN'];
const bizType = ref('PRODUCT_VERSION');
const bizId = ref('');
const loading = ref(false);
const rows = ref<ApprovalRecord[]>([]);
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);
const typeOptions = TYPES.map(item => ({ label: item, value: item }));

const columns = computed<DataTableColumns<ApprovalRecord>>(() => [
  { title: $t('page.mes.approvalRecord.action'), key: 'action', width: 110 },
  { title: $t('page.mes.approvalRecord.fromStatus'), key: 'fromStatus', width: 120 },
  { title: $t('page.mes.approvalRecord.toStatus'), key: 'toStatus', width: 120 },
  { title: $t('page.mes.approvalRecord.assignee'), key: 'assignee', width: 140 },
  { title: $t('page.mes.approvalRecord.operator'), key: 'operator', width: 140 },
  { title: $t('page.mes.approvalRecord.comment'), key: 'comment' },
  { title: $t('page.mes.approvalRecord.actionAt'), key: 'actionAt', width: 180 }
]);

async function fetchData() {
  const id = bizId.value.trim();
  if (!id) {
    rows.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const res = await getApprovalRecords({ bizType: bizType.value, bizId: id, page: page.value, pageSize: pageSize.value });
    rows.value = res.data?.records ?? [];
    total.value = res.data?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function search() {
  page.value = 1;
  fetchData();
}

function onPageUpdate(value: number) {
  page.value = value;
  fetchData();
}

function onSizeUpdate(value: number) {
  pageSize.value = value;
  page.value = 1;
  fetchData();
}
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.approvalRecord.title') }}</span>
          <div class="flex items-center gap-8px">
            <NSelect v-model:value="bizType" class="w-220px" :options="typeOptions" />
            <NInput v-model:value="bizId" class="w-220px" :placeholder="$t('page.mes.approvalRecord.bizId')" @keyup.enter="search" />
            <NButton type="primary" @click="search">{{ $t('common.search') }}</NButton>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.approvalRecord.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
      <div class="mt-12px flex justify-end">
        <NPagination
          :page="page"
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
