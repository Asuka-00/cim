<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { inputPackage } from '@/service/api/mes/package-input';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesPackageInput' });

const OPEN = new Set(['WAITING', 'RUNNING']);
const { hasAuth } = useAuth();
const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string }>({
    initialQuery: { lotNo: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [{ key: 'lotNo', label: $t('page.mes.packageInput.lotNo'), type: 'input' }]);
const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const sources = ref<Lot[]>([]);
const form = reactive({ sourceLotId: null as string | null, qty: 1 });

const rules = computed<FormRules>(() => ({
  sourceLotId: [{ required: true, message: $t('page.mes.packageInput.required'), trigger: 'change' }],
  qty: [{ required: true, type: 'number', message: $t('page.mes.packageInput.required'), trigger: 'blur' }]
}));

const sourceOptions = computed(() =>
  sources.value
    .filter(item => String(item.id) !== String(current.value?.id) && OPEN.has(item.status) && (item.holdCount ?? 0) === 0)
    .map(item => ({ label: `${item.lotNo} (${item.qty ?? 0})`, value: String(item.id) }))
);

function canInput(row: Lot) {
  return Boolean(row.woId) && OPEN.has(row.status) && hasAuth('mes:package:input');
}

async function openInput(row: Lot) {
  current.value = row;
  Object.assign(form, { sourceLotId: null, qty: 1 });
  const res = await getLotList({ page: 1, pageSize: 200 });
  sources.value = res.data?.records ?? [];
  showModal.value = true;
}

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.packageInput.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.packageInput.status'), key: 'status', width: 120 },
  { title: $t('page.mes.packageInput.qty'), key: 'qty', width: 100 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 110,
    render: row =>
      canInput(row)
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openInput(row) }, () =>
            $t('page.mes.packageInput.input')
          )
        : null
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  if (!current.value || !form.sourceLotId) return;
  await inputPackage(String(current.value.id), { sourceLotId: form.sourceLotId, qty: form.qty });
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
        <span>{{ $t('page.mes.packageInput.title') }}</span>
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
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.packageInput.inputTitle')" class="w-560px" :on-confirm="handleSubmit">
      <div class="mb-12px">{{ $t('page.mes.packageInput.hint') }}</div>
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.packageInput.source')" path="sourceLotId">
          <NSelect v-model:value="form.sourceLotId" :options="sourceOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.packageInput.qty')" path="qty">
          <NInputNumber v-model:value="form.qty" :min="1" class="w-full" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
