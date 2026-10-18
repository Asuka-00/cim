<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import type { DataTableColumns } from 'naive-ui';
import { getMyApprovals, type ApprovalTodo } from '@/service/api/mes/approval';
import { $t } from '@/locales';

defineOptions({ name: 'MesApproval' });

const loading = ref(false);
const rows = ref<ApprovalTodo[]>([]);

const columns = computed<DataTableColumns<ApprovalTodo>>(() => [
  { title: $t('page.mes.approval.bizType'), key: 'bizType' },
  { title: $t('page.mes.approval.bizId'), key: 'bizId', width: 180 },
  { title: $t('page.mes.approval.operator'), key: 'operator', width: 140 },
  { title: $t('common.remark'), key: 'comment' },
  { title: $t('page.mes.approval.actionAt'), key: 'actionAt', width: 180 }
]);

async function load() {
  loading.value = true;
  try {
    const { data } = await getMyApprovals();
    rows.value = data ?? [];
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.approval.title') }}</span>
          <NButton @click="load">{{ $t('page.mes.approval.reload') }}</NButton>
        </div>
      </template>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
  </div>
</template>
