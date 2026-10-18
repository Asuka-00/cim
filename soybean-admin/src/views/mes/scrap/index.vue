<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { scrapLot, scrapWafer } from '@/service/api/mes/scrap';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { getLotWafers, type LotWafer } from '@/service/api/mes/split';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import DictSelect from '@/components/business/dict-select.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesScrap' });

const OPEN = new Set(['WAITING', 'RUNNING']);
const ALIVE = new Set(['NORMAL', 'RUNNING']);
const { hasAuth } = useAuth();
const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string }>({
    initialQuery: { lotNo: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [{ key: 'lotNo', label: $t('page.mes.scrap.lotNo'), type: 'input' }]);
const showLot = ref(false);
const showWafer = ref(false);
const lotRef = ref<FormInst | null>(null);
const waferRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const wafers = ref<LotWafer[]>([]);
const lotForm = reactive({ reasonCode: null as string | null, scrapQty: null as number | null, remark: '' });
const waferForm = reactive({ waferId: null as string | null, reasonCode: null as string | null, remark: '' });

const lotRules = computed<FormRules>(() => ({
  reasonCode: [{ required: true, message: $t('page.mes.scrap.required'), trigger: 'change' }]
}));
const waferRules = computed<FormRules>(() => ({
  waferId: [{ required: true, message: $t('page.mes.scrap.required'), trigger: 'change' }],
  reasonCode: [{ required: true, message: $t('page.mes.scrap.required'), trigger: 'change' }]
}));

const waferOptions = computed(() =>
  wafers.value.filter(item => ALIVE.has(item.status ?? '')).map(item => ({ label: item.waferCode, value: String(item.id) }))
);

function canScrap(row: Lot) {
  return OPEN.has(row.status);
}

async function openLot(row: Lot) {
  current.value = row;
  Object.assign(lotForm, { reasonCode: null, scrapQty: null, remark: '' });
  showLot.value = true;
}

async function openWafer(row: Lot) {
  current.value = row;
  Object.assign(waferForm, { waferId: null, reasonCode: null, remark: '' });
  const res = await getLotWafers(String(row.id));
  wafers.value = res.data ?? [];
  showWafer.value = true;
}

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.scrap.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.scrap.status'), key: 'status', width: 120 },
  { title: $t('page.mes.scrap.qty'), key: 'qty', width: 100 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 160,
    render: row =>
      canScrap(row)
        ? h('div', { class: 'flex gap-8px' }, [
            hasAuth('mes:scrap:lot')
              ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => openLot(row) }, () => $t('page.mes.scrap.lot'))
              : null,
            hasAuth('mes:scrap:wafer')
              ? h(NButton, { size: 'small', quaternary: true, onClick: () => openWafer(row) }, () => $t('page.mes.scrap.wafer'))
              : null
          ])
        : null
  }
]);

async function submitLot() {
  await lotRef.value?.validate();
  if (!current.value || !lotForm.reasonCode) return;
  await scrapLot(String(current.value.id), {
    reasonCode: lotForm.reasonCode,
    scrapQty: lotForm.scrapQty,
    remark: lotForm.remark
  });
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

async function submitWafer() {
  await waferRef.value?.validate();
  if (!waferForm.waferId || !waferForm.reasonCode) return;
  await scrapWafer(waferForm.waferId, { reasonCode: waferForm.reasonCode, remark: waferForm.remark });
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
        <span>{{ $t('page.mes.scrap.title') }}</span>
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
    <CrudDialog v-model:show="showLot" :title="$t('page.mes.scrap.lotTitle')" class="w-560px" :on-confirm="submitLot">
      <div class="mb-12px">{{ $t('page.mes.scrap.lotHint') }}</div>
      <NForm ref="lotRef" :model="lotForm" :rules="lotRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.scrap.reason')" path="reasonCode">
          <DictSelect v-model:value="lotForm.reasonCode" dict-type="MES_SCRAP_REASON" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.scrap.qty')" path="scrapQty">
          <NInputNumber v-model:value="lotForm.scrapQty" :min="1" clearable class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('common.remark')" path="remark">
          <NInput v-model:value="lotForm.remark" type="textarea" :rows="2" />
        </NFormItem>
      </NForm>
    </CrudDialog>
    <CrudDialog v-model:show="showWafer" :title="$t('page.mes.scrap.waferTitle')" class="w-560px" :on-confirm="submitWafer">
      <div class="mb-12px">{{ $t('page.mes.scrap.waferHint') }}</div>
      <NForm ref="waferRef" :model="waferForm" :rules="waferRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.scrap.wafer')" path="waferId">
          <NSelect v-model:value="waferForm.waferId" :options="waferOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.scrap.reason')" path="reasonCode">
          <DictSelect v-model:value="waferForm.reasonCode" dict-type="MES_SCRAP_REASON" filterable />
        </NFormItem>
        <NFormItem :label="$t('common.remark')" path="remark">
          <NInput v-model:value="waferForm.remark" type="textarea" :rows="2" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
