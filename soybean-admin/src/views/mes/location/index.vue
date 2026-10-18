<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import type { DataTableColumns, FormInst, FormRules } from 'naive-ui';
import {
  createLocation,
  createWarehouse,
  getLocations,
  getWarehouses,
  type StorageLocation,
  type Warehouse
} from '@/service/api/mes/location';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { $t } from '@/locales';

defineOptions({ name: 'MesLocation' });

const loading = ref(false);
const warehouses = ref<Warehouse[]>([]);
const locations = ref<StorageLocation[]>([]);
const showWarehouse = ref(false);
const showLocation = ref(false);
const warehouseFormRef = ref<FormInst | null>(null);
const locationFormRef = ref<FormInst | null>(null);
const warehouseForm = reactive({ whCode: '', whName: '', whType: 'RAW' });
const locationForm = reactive({ whId: null as string | null, locCode: '', locName: '' });

const warehouseRules = computed<FormRules>(() => ({
  whCode: [{ required: true, message: $t('page.mes.location.required'), trigger: 'blur' }],
  whName: [{ required: true, message: $t('page.mes.location.required'), trigger: 'blur' }]
}));
const locationRules = computed<FormRules>(() => ({
  whId: [{ required: true, message: $t('page.mes.location.required'), trigger: 'change' }],
  locCode: [{ required: true, message: $t('page.mes.location.required'), trigger: 'blur' }]
}));

const typeOptions = ['RAW', 'LINESIDE', 'QUARANTINE', 'OTHER'].map(value => ({ label: value, value }));
const warehouseOptions = computed(() =>
  warehouses.value.filter(item => item.enabled === 1).map(item => ({ label: `${item.whCode} ${item.whName}`, value: String(item.id) }))
);
const warehouseName = computed(() => {
  const names = new Map(warehouses.value.map(item => [String(item.id), item.whCode]));
  return (whId: string) => names.get(String(whId)) ?? whId;
});

const columns = computed<DataTableColumns<StorageLocation>>(() => [
  { title: $t('page.mes.location.id'), key: 'id', width: 180 },
  { title: $t('page.mes.location.locCode'), key: 'locCode' },
  { title: $t('page.mes.location.locName'), key: 'locName' },
  {
    title: $t('page.mes.location.warehouse'),
    key: 'whId',
    render: row => warehouseName.value(String(row.whId))
  }
]);

async function fetchData() {
  loading.value = true;
  try {
    const [warehouseRes, locationRes] = await Promise.all([getWarehouses(), getLocations()]);
    warehouses.value = warehouseRes.data ?? [];
    locations.value = locationRes.data ?? [];
  } finally {
    loading.value = false;
  }
}

function openWarehouse() {
  Object.assign(warehouseForm, { whCode: '', whName: '', whType: 'RAW' });
  showWarehouse.value = true;
}

function openLocation() {
  Object.assign(locationForm, { whId: null, locCode: '', locName: '' });
  showLocation.value = true;
}

async function submitWarehouse() {
  await warehouseFormRef.value?.validate();
  await createWarehouse({ ...warehouseForm });
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
}

async function submitLocation() {
  await locationFormRef.value?.validate();
  if (!locationForm.whId) return;
  await createLocation({ whId: locationForm.whId, locCode: locationForm.locCode, locName: locationForm.locName });
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.location.title') }}</span>
          <div class="flex gap-8px">
            <PermissionGuard code="mes:warehouse:add">
              <NButton @click="openWarehouse">{{ $t('page.mes.location.addWarehouse') }}</NButton>
            </PermissionGuard>
            <PermissionGuard code="mes:location:add">
              <NButton type="primary" @click="openLocation">{{ $t('page.mes.location.addLocation') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <DataTable flex-height :columns="columns" :data="locations" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showWarehouse" :title="$t('page.mes.location.addWarehouse')" class="w-560px" :on-confirm="submitWarehouse">
      <NForm ref="warehouseFormRef" :model="warehouseForm" :rules="warehouseRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.location.whCode')" path="whCode">
          <NInput v-model:value="warehouseForm.whCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.location.whName')" path="whName">
          <NInput v-model:value="warehouseForm.whName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.location.whType')" path="whType">
          <NSelect v-model:value="warehouseForm.whType" :options="typeOptions" />
        </NFormItem>
      </NForm>
    </CrudDialog>
    <CrudDialog v-model:show="showLocation" :title="$t('page.mes.location.addLocation')" class="w-560px" :on-confirm="submitLocation">
      <NForm ref="locationFormRef" :model="locationForm" :rules="locationRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.location.warehouse')" path="whId">
          <NSelect v-model:value="locationForm.whId" :options="warehouseOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.location.locCode')" path="locCode">
          <NInput v-model:value="locationForm.locCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.location.locName')" path="locName">
          <NInput v-model:value="locationForm.locName" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
