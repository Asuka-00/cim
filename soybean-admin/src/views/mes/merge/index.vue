<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { mergeLot } from '@/service/api/mes/merge';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesMerge' });

const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string; status: string }>({
    initialQuery: { lotNo: '', status: 'WAITING' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'lotNo', label: $t('page.mes.merge.lotNo'), type: 'input' }
]);

const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const sources = ref<Lot[]>([]);
const form = reactive({ sourceLotId: null as string | null });

const rules = computed<FormRules>(() => ({
  sourceLotId: [{ required: true, message: $t('page.mes.merge.required'), trigger: 'change' }]
}));

const sourceOptions = computed(() =>
  sources.value
    .filter(item => !current.value || String(item.id) !== String(current.value.id))
    .map(item => ({ label: item.lotNo, value: String(item.id) }))
);

async function openMerge(row: Lot) {
  current.value = row;
  form.sourceLotId = null;
  const page = await getLotList({ page: 1, pageSize: 200, status: 'WAITING' });
  sources.value = page.data?.records ?? [];
  showModal.value = true;
}

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.merge.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.merge.status'), key: 'status', width: 120 },
  { title: $t('page.mes.merge.qty'), key: 'qty', width: 100 },
  { title: $t('page.mes.merge.holdCount'), key: 'holdCount', width: 90 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 100,
    render: row =>
      row.status === 'WAITING' && (row.holdCount ?? 0) === 0 && hasAuth('mes:lot:merge')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openMerge(row) }, () =>
            $t('page.mes.merge.merge')
          )
        : null
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  if (!current.value || !form.sourceLotId) return;
  await mergeLot(String(current.value.id), { sourceLotId: form.sourceLotId });
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
        <span>{{ $t('page.mes.merge.title') }}</span>
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
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.merge.mergeTitle')" class="w-560px" :on-confirm="handleSubmit">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.merge.source')" path="sourceLotId">
          <NSelect v-model:value="form.sourceLotId" :options="sourceOptions" filterable />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
