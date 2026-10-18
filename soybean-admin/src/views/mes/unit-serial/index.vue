<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { assignUnitSerial, getUnitSerials, type UnitSerial } from '@/service/api/mes/unit-serial';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { getPackContainers, type PackContainer } from '@/service/api/mes/pack-container';
import { getStrips, type Strip } from '@/service/api/mes/strip';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesUnitSerial' });

const { hasAuth } = useAuth();
const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Lot, { lotNo: string }>({
    initialQuery: { lotNo: '' },
    fetcher: getLotList
  });

const filterFields = computed<FilterField[]>(() => [{ key: 'lotNo', label: $t('page.mes.unitSerial.lotNo'), type: 'input' }]);
const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const current = ref<Lot | null>(null);
const serials = ref<UnitSerial[]>([]);
const strips = ref<Strip[]>([]);
const containers = ref<PackContainer[]>([]);
const form = reactive({ serialNo: '', stripId: null as string | null, containerId: null as string | null, position: '' });

const rules = computed<FormRules>(() => ({
  serialNo: [{ required: true, message: $t('page.mes.unitSerial.required'), trigger: 'blur' }]
}));

const stripOptions = computed(() => strips.value.map(item => ({ label: item.stripCode, value: String(item.id) })));
const containerOptions = computed(() => containers.value.map(item => ({ label: item.containerNo, value: String(item.id) })));

const columns = computed<DataTableColumns<Lot>>(() => [
  { title: $t('page.mes.unitSerial.lotNo'), key: 'lotNo' },
  { title: $t('page.mes.unitSerial.status'), key: 'status', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 110,
    render: row =>
      hasAuth('mes:unit-serial:add')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openLot(row) }, () => $t('page.mes.unitSerial.add'))
        : null
  }
]);

const serialColumns = computed<DataTableColumns<UnitSerial>>(() => [
  { title: $t('page.mes.unitSerial.serialNo'), key: 'serialNo' },
  { title: $t('page.mes.unitSerial.status'), key: 'status', width: 110 },
  { title: $t('page.mes.unitSerial.position'), key: 'position' }
]);

async function loadSerials(lotId: string) {
  const res = await getUnitSerials(lotId);
  serials.value = res.data ?? [];
}

async function openLot(row: Lot) {
  current.value = row;
  Object.assign(form, { serialNo: '', stripId: null, containerId: null, position: '' });
  const [stripRes, containerRes] = await Promise.all([getStrips(String(row.id)), getPackContainers(String(row.id))]);
  strips.value = stripRes.data ?? [];
  containers.value = containerRes.data ?? [];
  await loadSerials(String(row.id));
  showModal.value = true;
}

async function handleSubmit() {
  await formRef.value?.validate();
  if (!current.value) return false;
  await assignUnitSerial(String(current.value.id), {
    serialNo: form.serialNo,
    stripId: form.stripId,
    containerId: form.containerId,
    position: form.position
  });
  window.$message?.success($t('common.addSuccess'));
  form.serialNo = '';
  form.position = '';
  await loadSerials(String(current.value.id));
  return false;
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
        <span>{{ $t('page.mes.unitSerial.title') }}</span>
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
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.unitSerial.addTitle')" class="w-720px" :on-confirm="handleSubmit">
      <DataTable :columns="serialColumns" :data="serials" size="small" class="mb-12px" />
      <div class="mb-12px">{{ $t('page.mes.unitSerial.hint') }}</div>
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.unitSerial.serialNo')" path="serialNo">
          <NInput v-model:value="form.serialNo" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.unitSerial.strip')" path="stripId">
          <NSelect v-model:value="form.stripId" :options="stripOptions" clearable filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.unitSerial.container')" path="containerId">
          <NSelect v-model:value="form.containerId" :options="containerOptions" clearable filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.unitSerial.position')" path="position">
          <NInput v-model:value="form.position" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
