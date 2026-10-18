<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getFutureActions, scheduleFutureAction, type FutureAction } from '@/service/api/mes/future-action';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesFutureAction' });

const TYPES = ['HOLD', 'CHANGE_PRIORITY'];
const { hasAuth } = useAuth();
const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string }>({
    initialQuery: { lotNo: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [{ key: 'lotNo', label: $t('page.mes.future.lotNo'), type: 'input' }]);
const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const actions = ref<FutureAction[]>([]);
const form = reactive({ routeSeqNo: 1, passNo: null as number | null, actionType: 'HOLD', actionParams: '' });

const rules = computed<FormRules>(() => ({
  routeSeqNo: [{ required: true, type: 'number', message: $t('page.mes.future.required'), trigger: 'blur' }],
  actionType: [{ required: true, message: $t('page.mes.future.required'), trigger: 'change' }],
  actionParams: [{ required: true, message: $t('page.mes.future.required'), trigger: 'blur' }]
}));
const typeOptions = TYPES.map(item => ({ label: item, value: item }));

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.future.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.future.status'), key: 'status', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 100,
    render: row =>
      hasAuth('mes:future:add')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openLot(row) }, () => $t('page.mes.future.add'))
        : null
  }
]);

const actionColumns = computed<DataTableColumns<FutureAction>>(() => [
  { title: $t('page.mes.future.seq'), key: 'routeSeqNo', width: 80 },
  { title: $t('page.mes.future.pass'), key: 'passNo', width: 80 },
  { title: $t('page.mes.future.type'), key: 'actionType', width: 160 },
  { title: $t('page.mes.future.params'), key: 'actionParams' },
  { title: $t('page.mes.future.status'), key: 'status', width: 110 }
]);

async function loadActions(lotId: string) {
  const res = await getFutureActions(lotId);
  actions.value = res.data ?? [];
}

async function openLot(row: Lot) {
  current.value = row;
  Object.assign(form, { routeSeqNo: 1, passNo: null, actionType: 'HOLD', actionParams: '' });
  await loadActions(String(row.id));
  showModal.value = true;
}

async function handleSubmit() {
  await formRef.value?.validate();
  if (!current.value) return false;
  if (form.actionType === 'CHANGE_PRIORITY' && !/^[1-9]$/.test(form.actionParams.trim())) {
    window.$message?.warning($t('page.mes.future.priorityHint'));
    return false;
  }
  await scheduleFutureAction(String(current.value.id), {
    routeSeqNo: form.routeSeqNo,
    passNo: form.passNo,
    actionType: form.actionType,
    actionParams: form.actionParams.trim()
  });
  window.$message?.success($t('common.addSuccess'));
  form.actionParams = '';
  await loadActions(String(current.value.id));
  return false;
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
        <span>{{ $t('page.mes.future.title') }}</span>
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
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.future.addTitle')" class="w-760px" :on-confirm="handleSubmit">
      <DataTable :columns="actionColumns" :data="actions" size="small" class="mb-12px" />
      <div class="mb-12px">{{ $t('page.mes.future.hint') }}</div>
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.future.seq')" path="routeSeqNo">
          <NInputNumber v-model:value="form.routeSeqNo" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.future.pass')" path="passNo">
          <NInputNumber v-model:value="form.passNo" :min="1" clearable class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.future.type')" path="actionType">
          <NSelect v-model:value="form.actionType" :options="typeOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.future.params')" path="actionParams">
          <NInput v-model:value="form.actionParams" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
