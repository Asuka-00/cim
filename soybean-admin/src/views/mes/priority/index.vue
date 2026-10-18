<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { changeLotPriority } from '@/service/api/mes/priority';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesPriority' });

const OPEN = new Set(['WAITING', 'RUNNING']);
const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string; status: string }>({
    initialQuery: { lotNo: '', status: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'lotNo', label: $t('page.mes.priority.lotNo'), type: 'input' }
]);

const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const form = reactive({ priority: 5 });

const rules = computed<FormRules>(() => ({
  priority: [{ required: true, type: 'number', message: $t('page.mes.priority.required'), trigger: 'blur' }]
}));

function openChange(row: Lot) {
  current.value = row;
  form.priority = row.priority && row.priority >= 1 && row.priority <= 9 ? row.priority : 5;
  showModal.value = true;
}

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.priority.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.priority.status'), key: 'status', width: 120 },
  { title: $t('page.mes.priority.priority'), key: 'priority', width: 100 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 100,
    render: row =>
      OPEN.has(row.status) && hasAuth('mes:lot:priority')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openChange(row) }, () =>
            $t('page.mes.priority.change')
          )
        : null
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  if (!current.value) return;
  await changeLotPriority(String(current.value.id), { priority: form.priority });
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
        <span>{{ $t('page.mes.priority.title') }}</span>
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
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.priority.changeTitle')" class="w-560px" :on-confirm="handleSubmit">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.priority.priority')" path="priority">
          <NInputNumber v-model:value="form.priority" :min="1" :max="9" class="w-full" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
