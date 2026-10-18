<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { createPackContainer, getPackContainers, packContainer, type PackContainer } from '@/service/api/mes/pack-container';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesPackContainer' });

const OPEN = new Set(['WAITING', 'RUNNING']);
const TYPES = ['TRAY', 'REEL', 'TUBE', 'BOX'];
const { hasAuth } = useAuth();
const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string }>({
    initialQuery: { lotNo: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [{ key: 'lotNo', label: $t('page.mes.pack.lotNo'), type: 'input' }]);
const showCreate = ref(false);
const showPack = ref(false);
const createRef = ref<FormInst | null>(null);
const packRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const packing = ref<PackContainer | null>(null);
const containers = ref<PackContainer[]>([]);
const createForm = reactive({ containerNo: '', containerType: 'TRAY', capacity: 1 });
const packForm = reactive({ qty: 1 });

const createRules = computed<FormRules>(() => ({
  containerNo: [{ required: true, message: $t('page.mes.pack.required'), trigger: 'blur' }],
  containerType: [{ required: true, message: $t('page.mes.pack.required'), trigger: 'change' }],
  capacity: [{ required: true, type: 'number', message: $t('page.mes.pack.required'), trigger: 'blur' }]
}));
const packRules = computed<FormRules>(() => ({
  qty: [{ required: true, type: 'number', message: $t('page.mes.pack.required'), trigger: 'blur' }]
}));
const typeOptions = TYPES.map(item => ({ label: item, value: item }));

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.pack.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.pack.status'), key: 'status', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 110,
    render: row =>
      OPEN.has(row.status) && hasAuth('mes:pack:add')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openCreate(row) }, () => $t('page.mes.pack.open'))
        : null
  }
]);

const containerColumns = computed<DataTableColumns<PackContainer>>(() => [
  { title: $t('page.mes.pack.no'), key: 'containerNo' },
  { title: $t('page.mes.pack.type'), key: 'containerType', width: 100 },
  { title: $t('page.mes.pack.status'), key: 'status', width: 100 },
  { title: $t('page.mes.pack.qty'), key: 'qty', width: 80 },
  { title: $t('page.mes.pack.capacity'), key: 'capacity', width: 80 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 90,
    render: row =>
      row.status === 'OPEN' && hasAuth('mes:pack:pack')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openPack(row) }, () => $t('page.mes.pack.pack'))
        : null
  }
]);

async function loadContainers(lotId: string) {
  const res = await getPackContainers(lotId);
  containers.value = res.data ?? [];
}

async function openCreate(row: Lot) {
  current.value = row;
  Object.assign(createForm, { containerNo: '', containerType: 'TRAY', capacity: 1 });
  await loadContainers(String(row.id));
  showCreate.value = true;
}

function openPack(row: PackContainer) {
  packing.value = row;
  packForm.qty = 1;
  showPack.value = true;
}

async function submitCreate() {
  await createRef.value?.validate();
  if (!current.value) return false;
  await createPackContainer(String(current.value.id), createForm);
  window.$message?.success($t('common.addSuccess'));
  createForm.containerNo = '';
  await loadContainers(String(current.value.id));
  return false;
}

async function submitPack() {
  await packRef.value?.validate();
  if (!packing.value || !current.value) return;
  await packContainer(String(packing.value.id), { qty: packForm.qty });
  window.$message?.success($t('common.saveSuccess'));
  await loadContainers(String(current.value.id));
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
        <span>{{ $t('page.mes.pack.title') }}</span>
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
    <CrudDialog v-model:show="showCreate" :title="$t('page.mes.pack.addTitle')" class="w-760px" :on-confirm="submitCreate">
      <DataTable :columns="containerColumns" :data="containers" size="small" class="mb-12px" />
      <div class="mb-12px">{{ $t('page.mes.pack.hint') }}</div>
      <NForm ref="createRef" :model="createForm" :rules="createRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.pack.no')" path="containerNo">
          <NInput v-model:value="createForm.containerNo" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.pack.type')" path="containerType">
          <NSelect v-model:value="createForm.containerType" :options="typeOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.pack.capacity')" path="capacity">
          <NInputNumber v-model:value="createForm.capacity" :min="1" class="w-full" />
        </NFormItem>
      </NForm>
    </CrudDialog>
    <CrudDialog v-model:show="showPack" :title="$t('page.mes.pack.packTitle')" class="w-480px" :on-confirm="submitPack">
      <NForm ref="packRef" :model="packForm" :rules="packRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.pack.qty')" path="qty">
          <NInputNumber v-model:value="packForm.qty" :min="1" class="w-full" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
