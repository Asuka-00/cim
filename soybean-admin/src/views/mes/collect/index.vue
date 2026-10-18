<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { correctCollect, getEffectiveCollect, recordCollect, voidCollect } from '@/service/api/mes/collect';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesCollect' });

const { hasAuth } = useAuth();
const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string; status: string }>({
    initialQuery: { lotNo: '', status: 'RUNNING' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [{ key: 'lotNo', label: $t('page.mes.collect.lotNo'), type: 'input' }]);
const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const effective = ref<string>('');
const form = reactive({ itemId: '', valueNum: null as number | null, routeSeqNo: 1, passNo: 1 });

const rules = computed<FormRules>(() => ({
  itemId: [{ required: true, message: $t('page.mes.collect.required'), trigger: 'blur' }],
  valueNum: [{ required: true, type: 'number', message: $t('page.mes.collect.required'), trigger: 'blur' }]
}));

function openLot(row: Lot) {
  current.value = row;
  Object.assign(form, { itemId: '', valueNum: null, routeSeqNo: row.routeSeqNo ?? 1, passNo: 1 });
  effective.value = '';
  showModal.value = true;
}

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.collect.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.collect.status'), key: 'status', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 100,
    render: row =>
      row.status === 'RUNNING' && hasAuth('mes:collect:record')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openLot(row) }, () => $t('page.mes.collect.open'))
        : null
  }
]);

function payload() {
  return { lotId: String(current.value?.id), itemId: form.itemId, valueNum: form.valueNum ?? 0 };
}

async function handleRecord() {
  await formRef.value?.validate();
  if (!current.value) return;
  await recordCollect(payload());
  window.$message?.success($t('common.saveSuccess'));
  return false;
}

async function handleCorrect() {
  await formRef.value?.validate();
  if (!current.value || !hasAuth('mes:collect:correct')) return;
  await correctCollect(payload());
  window.$message?.success($t('common.saveSuccess'));
}

async function handleVoid() {
  if (!current.value || !form.itemId || !hasAuth('mes:collect:void')) return;
  await voidCollect({ lotId: String(current.value.id), itemId: form.itemId });
  window.$message?.success($t('common.saveSuccess'));
}

async function handleEffective() {
  if (!current.value || !form.itemId) return;
  const res = await getEffectiveCollect({
    lotId: String(current.value.id),
    itemId: form.itemId,
    routeSeqNo: form.routeSeqNo,
    passNo: form.passNo
  });
  effective.value = res.data == null ? $t('page.mes.collect.empty') : String(res.data);
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
        <span>{{ $t('page.mes.collect.title') }}</span>
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
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.collect.recordTitle')" class="w-640px" :on-confirm="handleRecord">
      <div class="mb-12px">{{ $t('page.mes.collect.hint') }}</div>
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.collect.itemId')" path="itemId">
          <NInput v-model:value="form.itemId" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.collect.value')" path="valueNum">
          <NInputNumber v-model:value="form.valueNum" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.collect.seq')" path="routeSeqNo">
          <NInputNumber v-model:value="form.routeSeqNo" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.collect.pass')" path="passNo">
          <NInputNumber v-model:value="form.passNo" :min="1" class="w-full" />
        </NFormItem>
      </NForm>
      <div class="mb-12px flex gap-8px">
        <NButton v-if="hasAuth('mes:collect:correct')" @click="handleCorrect">{{ $t('page.mes.collect.correct') }}</NButton>
        <NButton v-if="hasAuth('mes:collect:void')" @click="handleVoid">{{ $t('page.mes.collect.void') }}</NButton>
        <NButton @click="handleEffective">{{ $t('page.mes.collect.effective') }}</NButton>
      </div>
      <div v-if="effective">{{ $t('page.mes.collect.effectiveValue', { value: effective }) }}</div>
    </CrudDialog>
  </div>
</template>
