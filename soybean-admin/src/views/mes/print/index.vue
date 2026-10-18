<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { getLotPrints, recordLotPrint, type LotPrint } from '@/service/api/mes/print';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesPrint' });

const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string; status: string }>({
    initialQuery: { lotNo: '', status: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'lotNo', label: $t('page.mes.print.lotNo'), type: 'input' }
]);

const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const prints = ref<LotPrint[]>([]);
const form = reactive({ tplCode: '', copies: 1 });

const rules = computed<FormRules>(() => ({
  tplCode: [{ required: true, message: $t('page.mes.print.required'), trigger: 'blur' }],
  copies: [{ required: true, type: 'number', message: $t('page.mes.print.required'), trigger: 'blur' }]
}));

const printColumns = computed<DataTableColumns<LotPrint>>(() => [
  { title: $t('page.mes.print.printedAt'), key: 'printedAt', width: 170 },
  { title: $t('page.mes.print.copies'), key: 'copies', width: 80 },
  {
    title: $t('page.mes.print.reprint'),
    key: 'reprint',
    width: 80,
    render: row => (row.reprint === 1 ? $t('page.mes.print.yes') : '')
  }
]);

async function openPrint(row: Lot) {
  current.value = row;
  form.tplCode = '';
  form.copies = 1;
  const res = await getLotPrints(String(row.id));
  prints.value = res.data ?? [];
  showModal.value = true;
}

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.print.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.print.status'), key: 'status', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 100,
    render: row =>
      hasAuth('mes:lot:print')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openPrint(row) }, () => $t('page.mes.print.open'))
        : null
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  if (!current.value) return false;
  await recordLotPrint(String(current.value.id), { tplCode: form.tplCode.trim(), copies: form.copies });
  window.$message?.success($t('common.saveSuccess'));
  const res = await getLotPrints(String(current.value.id));
  prints.value = res.data ?? [];
  form.tplCode = '';
  form.copies = 1;
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
        <span>{{ $t('page.mes.print.title') }}</span>
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
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.print.openTitle')" class="w-640px" :on-confirm="handleSubmit">
      <DataTable :columns="printColumns" :data="prints" size="small" class="mb-12px" />
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.print.tplCode')" path="tplCode">
          <NInput v-model:value="form.tplCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.print.copies')" path="copies">
          <NInputNumber v-model:value="form.copies" :min="1" class="w-full" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
