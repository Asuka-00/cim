<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import type { FormInst, FormRules } from 'naive-ui';
import { getLotList, startLot, type Lot } from '@/service/api/mes/lot';
import { getWorkOrderList, type WorkOrder } from '@/service/api/mes/work-order';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { $t } from '@/locales';

defineOptions({ name: 'MesLot' });

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string; status: string }>({
    initialQuery: { lotNo: '', status: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'lotNo', label: $t('page.mes.lot.lotNo'), type: 'input' },
  { key: 'status', label: $t('page.mes.lot.status'), type: 'input' }
]);

const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const workOrders = ref<WorkOrder[]>([]);
const form = reactive({
  woId: null as string | null,
  lotNo: '',
  lotType: '',
  qty: null as number | null,
  remark: ''
});

const rules = computed<FormRules>(() => ({
  woId: [{ required: true, message: $t('page.mes.lot.required'), trigger: 'change' }],
  lotNo: [{ required: true, message: $t('page.mes.lot.required'), trigger: 'blur' }],
  qty: [{ required: true, type: 'number', message: $t('page.mes.lot.required'), trigger: 'blur' }]
}));

const workOrderOptions = computed(() =>
  workOrders.value.map(item => ({ label: item.woNo, value: String(item.id) }))
);

const columns = computed(() => [
  { title: $t('page.mes.lot.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.lot.status'), key: 'status', width: 120 },
  { title: $t('page.mes.lot.qty'), key: 'qty', width: 100 },
  { title: $t('page.mes.lot.routeSeqNo'), key: 'routeSeqNo', width: 110 },
  { title: $t('page.mes.lot.holdCount'), key: 'holdCount', width: 90 },
  { title: $t('page.mes.lot.priority'), key: 'priority', width: 90 }
]);

async function openStart() {
  Object.assign(form, { woId: null, lotNo: '', lotType: '', qty: null, remark: '' });
  const page = await getWorkOrderList({ page: 1, pageSize: 200 });
  workOrders.value = (page.data?.records ?? []).filter(item => item.status === 'RELEASED');
  showModal.value = true;
}

async function handleSubmit() {
  await formRef.value?.validate();
  await startLot(form.woId!, {
    lotNo: form.lotNo,
    lotType: form.lotType || undefined,
    qty: form.qty!,
    remark: form.remark
  });
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
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
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.lot.title') }}</span>
          <PermissionGuard code="mes:lot:start">
            <NButton type="primary" @click="openStart">{{ $t('page.mes.lot.start') }}</NButton>
          </PermissionGuard>
        </div>
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
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.lot.startTitle')" class="w-560px" :on-confirm="handleSubmit">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.lot.workOrder')" path="woId">
          <NSelect v-model:value="form.woId" :options="workOrderOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.lot.lotNo')" path="lotNo">
          <NInput v-model:value="form.lotNo" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.lot.lotType')" path="lotType">
          <NInput v-model:value="form.lotType" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.lot.qty')" path="qty">
          <NInputNumber v-model:value="form.qty" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('common.remark')" path="remark">
          <NInput v-model:value="form.remark" type="textarea" :rows="2" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
