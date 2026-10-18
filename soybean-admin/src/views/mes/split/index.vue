<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { getLotWafers, splitLot, type LotWafer } from '@/service/api/mes/split';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesSplit' });

const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string; status: string }>({
    initialQuery: { lotNo: '', status: 'WAITING' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'lotNo', label: $t('page.mes.split.lotNo'), type: 'input' }
]);

const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const wafers = ref<LotWafer[]>([]);
const form = reactive({ waferIds: [] as string[] });

const rules = computed<FormRules>(() => ({
  waferIds: [{ required: true, type: 'array', message: $t('page.mes.split.required'), trigger: 'change' }]
}));

const waferOptions = computed(() =>
  wafers.value.map(item => ({
    label: `${item.slotNo ?? '-'} ${item.waferCode}`,
    value: String(item.id)
  }))
);

async function openSplit(row: Lot) {
  current.value = row;
  form.waferIds = [];
  const res = await getLotWafers(String(row.id));
  wafers.value = res.data ?? [];
  showModal.value = true;
}

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.split.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.split.status'), key: 'status', width: 120 },
  { title: $t('page.mes.split.qty'), key: 'qty', width: 100 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 100,
    render: row =>
      row.status === 'WAITING' && hasAuth('mes:lot:split')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openSplit(row) }, () =>
            $t('page.mes.split.split')
          )
        : null
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  if (!current.value) return;
  const res = await splitLot(String(current.value.id), { waferIds: form.waferIds });
  window.$message?.success($t('page.mes.split.splitDone', { name: res.data ?? '' }));
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
        <span>{{ $t('page.mes.split.title') }}</span>
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
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.split.splitTitle')" class="w-560px" :on-confirm="handleSubmit">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.split.wafers')" path="waferIds">
          <NSelect v-model:value="form.waferIds" :options="waferOptions" multiple />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
