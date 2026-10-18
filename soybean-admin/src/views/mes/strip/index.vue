<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { convertStrips, createStrip, getStrips, type Strip } from '@/service/api/mes/strip';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { getUomList, type Uom } from '@/service/api/mes/uom';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesStrip' });

const OPEN = new Set(['WAITING', 'RUNNING']);
const { hasAuth } = useAuth();
const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string }>({
    initialQuery: { lotNo: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [{ key: 'lotNo', label: $t('page.mes.strip.lotNo'), type: 'input' }]);
const showCreate = ref(false);
const showConvert = ref(false);
const createRef = ref<FormInst | null>(null);
const convertRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const strips = ref<Strip[]>([]);
const uoms = ref<Uom[]>([]);
const createForm = reactive({ stripCode: '', unitQty: 1, goodQty: null as number | null });
const convertForm = reactive({ unitId: null as string | null });

const createRules = computed<FormRules>(() => ({
  stripCode: [{ required: true, message: $t('page.mes.strip.required'), trigger: 'blur' }],
  unitQty: [{ required: true, type: 'number', message: $t('page.mes.strip.required'), trigger: 'blur' }]
}));
const convertRules = computed<FormRules>(() => ({
  unitId: [{ required: true, message: $t('page.mes.strip.required'), trigger: 'change' }]
}));

const uomOptions = computed(() => uoms.value.map(item => ({ label: `${item.uomCode} ${item.uomName}`, value: String(item.id) })));
const hasNormal = computed(() => strips.value.some(item => item.status === 'NORMAL'));

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.strip.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.strip.status'), key: 'status', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 180,
    render: row =>
      OPEN.has(row.status)
        ? h('div', { class: 'flex gap-8px' }, [
            hasAuth('mes:strip:add')
              ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openCreate(row) }, () => $t('page.mes.strip.add'))
              : null,
            hasAuth('mes:strip:convert')
              ? h(NButton, { size: 'small', quaternary: true, onClick: () => openConvert(row) }, () => $t('page.mes.strip.convert'))
              : null
          ])
        : null
  }
]);

const stripColumns = computed<DataTableColumns<Strip>>(() => [
  { title: $t('page.mes.strip.code'), key: 'stripCode' },
  { title: $t('page.mes.strip.status'), key: 'status', width: 120 },
  { title: $t('page.mes.strip.unitQty'), key: 'unitQty', width: 90 },
  { title: $t('page.mes.strip.goodQty'), key: 'goodQty', width: 90 }
]);

async function loadStrips(lotId: string) {
  const res = await getStrips(lotId);
  strips.value = res.data ?? [];
}

async function openCreate(row: Lot) {
  current.value = row;
  Object.assign(createForm, { stripCode: '', unitQty: 1, goodQty: null });
  await loadStrips(String(row.id));
  showCreate.value = true;
}

async function openConvert(row: Lot) {
  current.value = row;
  convertForm.unitId = null;
  const [stripRes, uomPage] = await Promise.all([
    getStrips(String(row.id)),
    uoms.value.length === 0 ? getUomList({ page: 1, pageSize: 200 }) : Promise.resolve(null)
  ]);
  strips.value = stripRes.data ?? [];
  if (uomPage) uoms.value = uomPage.data?.records ?? [];
  showConvert.value = true;
}

async function submitCreate() {
  await createRef.value?.validate();
  if (!current.value) return false;
  await createStrip(String(current.value.id), {
    stripCode: createForm.stripCode,
    unitQty: createForm.unitQty,
    goodQty: createForm.goodQty ?? undefined
  });
  window.$message?.success($t('common.addSuccess'));
  createForm.stripCode = '';
  await loadStrips(String(current.value.id));
  return false;
}

async function submitConvert() {
  await convertRef.value?.validate();
  if (!current.value || !convertForm.unitId) return;
  await convertStrips(String(current.value.id), { unitId: convertForm.unitId });
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
        <span>{{ $t('page.mes.strip.title') }}</span>
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
    <CrudDialog v-model:show="showCreate" :title="$t('page.mes.strip.addTitle')" class="w-720px" :on-confirm="submitCreate">
      <DataTable :columns="stripColumns" :data="strips" size="small" class="mb-12px" />
      <div class="mb-12px">{{ $t('page.mes.strip.hint') }}</div>
      <NForm ref="createRef" :model="createForm" :rules="createRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.strip.code')" path="stripCode">
          <NInput v-model:value="createForm.stripCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.strip.unitQty')" path="unitQty">
          <NInputNumber v-model:value="createForm.unitQty" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.strip.goodQty')" path="goodQty">
          <NInputNumber v-model:value="createForm.goodQty" :min="0" class="w-full" />
        </NFormItem>
      </NForm>
    </CrudDialog>
    <CrudDialog v-model:show="showConvert" :title="$t('page.mes.strip.convertTitle')" class="w-560px" :on-confirm="submitConvert">
      <div class="mb-12px">{{ hasNormal ? $t('page.mes.strip.convertHint') : $t('page.mes.strip.convertEmpty') }}</div>
      <NForm ref="convertRef" :model="convertForm" :rules="convertRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.strip.unit')" path="unitId">
          <NSelect v-model:value="convertForm.unitId" :options="uomOptions" filterable />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
