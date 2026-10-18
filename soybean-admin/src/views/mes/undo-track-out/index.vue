<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { undoTrackOut } from '@/service/api/mes/undo-track-out';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesUndoTrackOut' });

const CAN_UNDO = new Set(['WAITING', 'COMPLETED']);
const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string; status: string }>({
    initialQuery: { lotNo: '', status: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'lotNo', label: $t('page.mes.undoTrackOut.lotNo'), type: 'input' },
  { key: 'status', label: $t('page.mes.undoTrackOut.status'), type: 'input' }
]);

const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const form = reactive({ reasonCode: '', remark: '', toolId: '' });

const rules = computed<FormRules>(() => ({
  reasonCode: [{ required: true, message: $t('page.mes.undoTrackOut.required'), trigger: 'blur' }]
}));

function openUndo(row: Lot) {
  current.value = row;
  Object.assign(form, { reasonCode: '', remark: '', toolId: '' });
  showModal.value = true;
}

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.undoTrackOut.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.undoTrackOut.status'), key: 'status', width: 120 },
  { title: $t('page.mes.undoTrackOut.qty'), key: 'qty', width: 100 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 120,
    render: row =>
      CAN_UNDO.has(row.status) && hasAuth('mes:lot:undo-track-out')
        ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => openUndo(row) }, () =>
            $t('page.mes.undoTrackOut.undo')
          )
        : null
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  if (!current.value) return;
  await undoTrackOut(String(current.value.id), {
    reasonCode: form.reasonCode,
    remark: form.remark,
    toolId: form.toolId.trim() || null
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
        <span>{{ $t('page.mes.undoTrackOut.title') }}</span>
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
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.undoTrackOut.undoTitle')" class="w-560px" :on-confirm="handleSubmit">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.undoTrackOut.reason')" path="reasonCode">
          <NInput v-model:value="form.reasonCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.undoTrackOut.toolId')" path="toolId">
          <NInput v-model:value="form.toolId" />
        </NFormItem>
        <NFormItem :label="$t('common.remark')" path="remark">
          <NInput v-model:value="form.remark" type="textarea" :rows="2" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
