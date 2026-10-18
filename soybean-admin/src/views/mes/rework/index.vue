<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { enterRework } from '@/service/api/mes/rework';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesRework' });

const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string; status: string }>({
    initialQuery: { lotNo: '', status: 'WAITING' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'lotNo', label: $t('page.mes.rework.lotNo'), type: 'input' }
]);

const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const form = reactive({ reworkId: '', reasonCode: '' });

const rules = computed<FormRules>(() => ({
  reworkId: [{ required: true, message: $t('page.mes.rework.required'), trigger: 'blur' }]
}));

function openEnter(row: Lot) {
  current.value = row;
  form.reworkId = '';
  form.reasonCode = '';
  showModal.value = true;
}

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.rework.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.rework.status'), key: 'status', width: 120 },
  { title: $t('page.mes.rework.routeSeqNo'), key: 'routeSeqNo', width: 110 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 100,
    render: row =>
      row.status === 'WAITING' && !row.reworkId && hasAuth('mes:lot:rework')
        ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => openEnter(row) }, () =>
            $t('page.mes.rework.enter')
          )
        : null
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  if (!current.value) return;
  await enterRework(String(current.value.id), {
    reworkId: form.reworkId.trim(),
    reasonCode: form.reasonCode.trim() || undefined
  });
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
        <span>{{ $t('page.mes.rework.title') }}</span>
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
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.rework.enterTitle')" class="w-560px" :on-confirm="handleSubmit">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.rework.reworkId')" path="reworkId">
          <NInput v-model:value="form.reworkId" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.rework.reason')" path="reasonCode">
          <NInput v-model:value="form.reasonCode" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
