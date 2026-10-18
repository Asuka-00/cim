<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getActiveHolds, getHoldReasonList, holdLot, releaseLotHold, type HoldReason, type LotHoldRow } from '@/service/api/mes/hold';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesHold' });

const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string; status: string }>({
    initialQuery: { lotNo: '', status: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'lotNo', label: $t('page.mes.hold.lotNo'), type: 'input' }
]);

const showHold = ref(false);
const showRelease = ref(false);
const holdFormRef = ref<FormInst | null>(null);
const releaseFormRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const reasons = ref<HoldReason[]>([]);
const activeHolds = ref<LotHoldRow[]>([]);
const holdForm = reactive({ reasonCode: null as string | null, remark: '' });
const releaseForm = reactive({ holdId: null as string | null, remark: '' });

const holdRules = computed<FormRules>(() => ({
  reasonCode: [{ required: true, message: $t('page.mes.hold.required'), trigger: 'change' }]
}));
const releaseRules = computed<FormRules>(() => ({
  holdId: [{ required: true, message: $t('page.mes.hold.required'), trigger: 'change' }]
}));

const reasonOptions = computed(() =>
  reasons.value.filter(item => item.enabled === 1).map(item => ({ label: `${item.reasonCode} ${item.reasonName}`, value: item.reasonCode }))
);
const holdOptions = computed(() =>
  activeHolds.value.map(item => ({
    label: `${item.reasonCode}${item.holdRemark ? ' ' + item.holdRemark : ''}`,
    value: String(item.id)
  }))
);

function openHold(row: Lot) {
  current.value = row;
  holdForm.reasonCode = null;
  holdForm.remark = '';
  showHold.value = true;
}

async function openRelease(row: Lot) {
  current.value = row;
  releaseForm.holdId = null;
  releaseForm.remark = '';
  const res = await getActiveHolds(String(row.id));
  activeHolds.value = res.data ?? [];
  showRelease.value = true;
}

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.hold.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.hold.status'), key: 'status', width: 120 },
  { title: $t('page.mes.hold.holdCount'), key: 'holdCount', width: 100 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 160,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        hasAuth('mes:lot:hold')
          ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => openHold(row) }, () => $t('page.mes.hold.hold'))
          : null,
        (row.holdCount ?? 0) > 0 && hasAuth('mes:lot:release')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openRelease(row) }, () =>
              $t('page.mes.hold.release')
            )
          : null
      ])
  }
]);

async function submitHold() {
  await holdFormRef.value?.validate();
  if (!current.value || !holdForm.reasonCode) return;
  await holdLot(String(current.value.id), { reasonCode: holdForm.reasonCode, remark: holdForm.remark });
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

async function submitRelease() {
  await releaseFormRef.value?.validate();
  if (!current.value || !releaseForm.holdId) return;
  await releaseLotHold(String(current.value.id), { holdId: releaseForm.holdId, remark: releaseForm.remark });
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

onMounted(async () => {
  const page = await getHoldReasonList({ page: 1, pageSize: 200 });
  reasons.value = page.data?.records ?? [];
  await fetchData();
});
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
        <span>{{ $t('page.mes.hold.title') }}</span>
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
    <CrudDialog v-model:show="showHold" :title="$t('page.mes.hold.holdTitle')" class="w-560px" :on-confirm="submitHold">
      <NForm ref="holdFormRef" :model="holdForm" :rules="holdRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.hold.reason')" path="reasonCode">
          <NSelect v-model:value="holdForm.reasonCode" :options="reasonOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('common.remark')" path="remark">
          <NInput v-model:value="holdForm.remark" type="textarea" :rows="2" />
        </NFormItem>
      </NForm>
    </CrudDialog>
    <CrudDialog v-model:show="showRelease" :title="$t('page.mes.hold.releaseTitle')" class="w-560px" :on-confirm="submitRelease">
      <NForm ref="releaseFormRef" :model="releaseForm" :rules="releaseRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.hold.holdRow')" path="holdId">
          <NSelect v-model:value="releaseForm.holdId" :options="holdOptions" />
        </NFormItem>
        <NFormItem :label="$t('common.remark')" path="remark">
          <NInput v-model:value="releaseForm.remark" type="textarea" :rows="2" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
