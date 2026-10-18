<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { forceMoveLot } from '@/service/api/mes/force-move';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesForceMove' });

const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string; status: string }>({
    initialQuery: { lotNo: '', status: 'WAITING' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'lotNo', label: $t('page.mes.forceMove.lotNo'), type: 'input' }
]);

const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const form = reactive({ routeSeqNo: null as number | null, reasonCode: '', remark: '' });

const rules = computed<FormRules>(() => ({
  routeSeqNo: [{ required: true, type: 'number', message: $t('page.mes.forceMove.required'), trigger: 'blur' }],
  reasonCode: [{ required: true, message: $t('page.mes.forceMove.required'), trigger: 'blur' }]
}));

function openMove(row: Lot) {
  current.value = row;
  form.routeSeqNo = null;
  form.reasonCode = '';
  form.remark = '';
  showModal.value = true;
}

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.forceMove.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.forceMove.status'), key: 'status', width: 120 },
  { title: $t('page.mes.forceMove.routeSeqNo'), key: 'routeSeqNo', width: 110 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 110,
    render: row =>
      row.status === 'WAITING' && !row.reworkId && hasAuth('mes:lot:force-move')
        ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => openMove(row) }, () =>
            $t('page.mes.forceMove.move')
          )
        : null
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  if (!current.value || form.routeSeqNo == null) return;
  await forceMoveLot(String(current.value.id), {
    routeSeqNo: form.routeSeqNo,
    reasonCode: form.reasonCode,
    remark: form.remark
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
        <span>{{ $t('page.mes.forceMove.title') }}</span>
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
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.forceMove.moveTitle')" class="w-560px" :on-confirm="handleSubmit">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.forceMove.targetSeq')" path="routeSeqNo">
          <NInputNumber v-model:value="form.routeSeqNo" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.forceMove.reason')" path="reasonCode">
          <NInput v-model:value="form.reasonCode" />
        </NFormItem>
        <NFormItem :label="$t('common.remark')" path="remark">
          <NInput v-model:value="form.remark" type="textarea" :rows="2" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
