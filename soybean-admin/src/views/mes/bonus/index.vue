<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { bonusLot } from '@/service/api/mes/bonus';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesBonus' });

const OPEN = new Set(['WAITING', 'RUNNING']);
const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string; status: string }>({
    initialQuery: { lotNo: '', status: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'lotNo', label: $t('page.mes.bonus.lotNo'), type: 'input' }
]);

const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const form = reactive({ qty: null as number | null });

const rules = computed<FormRules>(() => ({
  qty: [{ required: true, type: 'number', message: $t('page.mes.bonus.required'), trigger: 'blur' }]
}));

function openBonus(row: Lot) {
  current.value = row;
  form.qty = null;
  showModal.value = true;
}

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.bonus.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.bonus.status'), key: 'status', width: 120 },
  { title: $t('page.mes.bonus.qty'), key: 'qty', width: 100 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 100,
    render: row =>
      OPEN.has(row.status) && hasAuth('mes:lot:bonus')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openBonus(row) }, () =>
            $t('page.mes.bonus.bonus')
          )
        : null
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  if (!current.value || form.qty == null) return;
  await bonusLot(String(current.value.id), { qty: form.qty });
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
        <span>{{ $t('page.mes.bonus.title') }}</span>
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
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.bonus.bonusTitle')" class="w-560px" :on-confirm="handleSubmit">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.bonus.addQty')" path="qty">
          <NInputNumber v-model:value="form.qty" :min="1" class="w-full" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
