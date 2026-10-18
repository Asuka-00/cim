<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getEquipmentBindings, unmountBinding, type EquipmentBinding } from '@/service/api/mes/binding';
import { getEquipmentList, type Equipment } from '@/service/api/mes/equipment';
import { getLocations, type StorageLocation } from '@/service/api/mes/location';
import { getMaterialList, type Material } from '@/service/api/mes/material';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesBinding' });

const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<EquipmentBinding[]>([]);
const equipment = ref<Equipment[]>([]);
const materials = ref<Material[]>([]);
const locations = ref<StorageLocation[]>([]);
const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const current = ref<EquipmentBinding | null>(null);
const form = reactive({ disposition: 'RETURN_LINESIDE', locationId: null as string | null });

const rules = computed<FormRules>(() => ({
  disposition: [{ required: true, message: $t('page.mes.binding.required'), trigger: 'change' }],
  locationId:
    form.disposition === 'RETURN_LINESIDE'
      ? [{ required: true, message: $t('page.mes.binding.locationRequired'), trigger: 'change' }]
      : []
}));

const dispositionOptions = [
  { label: 'RETURN_LINESIDE', value: 'RETURN_LINESIDE' },
  { label: 'SCRAP', value: 'SCRAP' }
];
const equipmentName = computed(() => {
  const names = new Map(equipment.value.map(item => [String(item.id), `${item.equipmentCode} ${item.equipmentName}`]));
  return (id: string) => names.get(String(id)) ?? id;
});
const materialName = computed(() => {
  const names = new Map(materials.value.map(item => [String(item.id), `${item.materialCode} ${item.materialName}`]));
  return (id: string) => names.get(String(id)) ?? id;
});
const locationOptions = computed(() => locations.value.map(item => ({ label: item.locCode, value: String(item.id) })));

function openUnmount(row: EquipmentBinding) {
  current.value = row;
  form.disposition = 'RETURN_LINESIDE';
  form.locationId = null;
  showModal.value = true;
}

const columns = computed<DataTableColumns<EquipmentBinding>>(() => [
  { title: $t('page.mes.binding.equipment'), key: 'equipmentId', render: row => equipmentName.value(row.equipmentId) },
  { title: $t('page.mes.binding.material'), key: 'materialId', render: row => materialName.value(row.materialId) },
  { title: $t('page.mes.binding.remainingQty'), key: 'remainingQty', width: 110 },
  { title: $t('page.mes.binding.status'), key: 'status', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 100,
    render: row =>
      row.status !== 'UNMOUNTED' && hasAuth('mes:binding:unmount')
        ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => openUnmount(row) }, () =>
            $t('page.mes.binding.unmount')
          )
        : null
  }
]);

async function fetchData() {
  loading.value = true;
  try {
    const [bindingRes, equipmentPage, materialPage, locationRes] = await Promise.all([
      getEquipmentBindings(),
      getEquipmentList({ page: 1, pageSize: 200 }),
      getMaterialList({ page: 1, pageSize: 200 }),
      getLocations()
    ]);
    rows.value = bindingRes.data ?? [];
    equipment.value = equipmentPage.data?.records ?? [];
    materials.value = materialPage.data?.records ?? [];
    locations.value = locationRes.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function handleSubmit() {
  await formRef.value?.validate();
  if (!current.value) return;
  await unmountBinding(String(current.value.id), {
    disposition: form.disposition,
    locationId: form.disposition === 'RETURN_LINESIDE' ? form.locationId : null
  });
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <span>{{ $t('page.mes.binding.title') }}</span>
      </template>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.binding.unmountTitle')" class="w-560px" :on-confirm="handleSubmit">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.binding.disposition')" path="disposition">
          <NSelect v-model:value="form.disposition" :options="dispositionOptions" />
        </NFormItem>
        <NFormItem v-if="form.disposition === 'RETURN_LINESIDE'" :label="$t('page.mes.binding.location')" path="locationId">
          <NSelect v-model:value="form.locationId" :options="locationOptions" filterable />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
