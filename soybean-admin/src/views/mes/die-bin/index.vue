<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getDieBins, replaceDieBins, type DieBinLine } from '@/service/api/mes/die-bin';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesDieBin' });

const { hasAuth } = useAuth();
const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string }>({
    initialQuery: { lotNo: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [{ key: 'lotNo', label: $t('page.mes.dieBin.lotNo'), type: 'input' }]);
const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const locked = ref(false);
const lines = ref<DieBinLine[]>([]);
const form = reactive({ binCode: '', binName: '', good: true, dieQty: 0 });

const rules = computed<FormRules>(() => ({
  binCode: [{ required: true, message: $t('page.mes.dieBin.required'), trigger: 'blur' }]
}));

const goodQty = computed(() => lines.value.filter(line => line.good).reduce((sum, line) => sum + Number(line.dieQty || 0), 0));

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.dieBin.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.dieBin.status'), key: 'status', width: 120 },
  { title: $t('page.mes.dieBin.qty'), key: 'qty', width: 100 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 110,
    render: row =>
      hasAuth('mes:die-bin:edit')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openLot(row) }, () => $t('page.mes.dieBin.edit'))
        : null
  }
]);

const lineColumns = computed<DataTableColumns<DieBinLine>>(() => [
  { title: $t('page.mes.dieBin.binCode'), key: 'binCode' },
  { title: $t('page.mes.dieBin.binName'), key: 'binName' },
  { title: $t('page.mes.dieBin.good'), key: 'good', width: 80, render: row => (row.good ? $t('page.mes.dieBin.yes') : '') },
  { title: $t('page.mes.dieBin.dieQty'), key: 'dieQty', width: 90 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 80,
    render: row =>
      locked.value
        ? null
        : h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => removeLine(row.binCode) }, () => $t('common.delete'))
  }
]);

async function openLot(row: Lot) {
  current.value = row;
  Object.assign(form, { binCode: '', binName: '', good: true, dieQty: 0 });
  const res = await getDieBins(String(row.id));
  const rows = res.data ?? [];
  locked.value = rows.some(item => item.waferId != null && String(item.waferId) !== '0');
  lines.value = rows
    .filter(item => item.waferId == null || String(item.waferId) === '0')
    .map(item => ({ binCode: item.binCode, binName: item.binName ?? '', good: item.isGood === 1, dieQty: Number(item.dieQty) }));
  showModal.value = true;
}

async function addLine() {
  await formRef.value?.validate();
  if (lines.value.some(line => line.binCode === form.binCode.trim())) {
    window.$message?.warning($t('page.mes.dieBin.duplicate'));
    return;
  }
  lines.value = [...lines.value, { binCode: form.binCode.trim(), binName: form.binName, good: form.good, dieQty: form.dieQty }];
  form.binCode = '';
  form.binName = '';
}

function removeLine(binCode: string) {
  lines.value = lines.value.filter(line => line.binCode !== binCode);
}

async function handleSubmit() {
  if (!current.value || locked.value || lines.value.length === 0) return false;
  await replaceDieBins(String(current.value.id), lines.value);
  window.$message?.success($t('common.saveSuccess'));
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
        <span>{{ $t('page.mes.dieBin.title') }}</span>
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
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.dieBin.editTitle')" class="w-720px" :on-confirm="handleSubmit">
      <div class="mb-12px">{{ $t('page.mes.dieBin.goodHint', { good: goodQty, qty: current?.qty ?? 0 }) }}</div>
      <div v-if="locked" class="mb-12px">{{ $t('page.mes.dieBin.locked') }}</div>
      <DataTable :columns="lineColumns" :data="lines" size="small" class="mb-12px" />
      <NForm v-if="!locked" ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.dieBin.binCode')" path="binCode">
          <NInput v-model:value="form.binCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.dieBin.binName')" path="binName">
          <NInput v-model:value="form.binName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.dieBin.good')" path="good">
          <NSwitch v-model:value="form.good" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.dieBin.dieQty')" path="dieQty">
          <NInputNumber v-model:value="form.dieQty" :min="0" class="w-full" />
        </NFormItem>
        <NButton class="mb-12px" @click="addLine">{{ $t('page.mes.dieBin.addLine') }}</NButton>
      </NForm>
    </CrudDialog>
  </div>
</template>
