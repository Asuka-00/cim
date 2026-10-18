<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { closeEquipmentFault, getEquipmentFaults, openEquipmentFault, type EquipmentFault } from '@/service/api/mes/equipment-fault';
import { getEquipmentList, type Equipment } from '@/service/api/mes/equipment';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesEquipmentFault' });

const SEVERITY = ['MINOR', 'MAJOR', 'CRITICAL'];
const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<EquipmentFault[]>([]);
const equipment = ref<Equipment[]>([]);
const lots = ref<Lot[]>([]);
const showOpen = ref(false);
const showClose = ref(false);
const openRef = ref<FormInst | null>(null);
const closeRef = ref<FormInst | null>(null);
const current = ref<EquipmentFault | null>(null);
const openForm = reactive({
  faultNo: '',
  equipmentId: null as string | null,
  description: '',
  severity: 'MINOR',
  lotIds: [] as string[],
  downEquipment: false
});
const closeForm = reactive({ solution: '' });

const openRules = computed<FormRules>(() => ({
  faultNo: [{ required: true, message: $t('page.mes.fault.required'), trigger: 'blur' }],
  equipmentId: [{ required: true, message: $t('page.mes.fault.required'), trigger: 'change' }],
  description: [{ required: true, message: $t('page.mes.fault.required'), trigger: 'blur' }]
}));
const closeRules = computed<FormRules>(() => ({
  solution: [{ required: true, message: $t('page.mes.fault.required'), trigger: 'blur' }]
}));

const equipmentName = computed(() => {
  const names = new Map(equipment.value.map(item => [String(item.id), `${item.equipmentCode} ${item.equipmentName}`]));
  return (id?: string | null) => (id ? (names.get(String(id)) ?? id) : '');
});
const equipmentOptions = computed(() =>
  equipment.value.map(item => ({ label: `${item.equipmentCode} ${item.equipmentName}`, value: String(item.id) }))
);
const lotOptions = computed(() => lots.value.map(item => ({ label: item.lotNo, value: String(item.id) })));
const severityOptions = SEVERITY.map(item => ({ label: item, value: item }));

function openCreate() {
  Object.assign(openForm, { faultNo: '', equipmentId: null, description: '', severity: 'MINOR', lotIds: [], downEquipment: false });
  showOpen.value = true;
}

function openClose(row: EquipmentFault) {
  current.value = row;
  closeForm.solution = '';
  showClose.value = true;
}

const columns = computed<DataTableColumns<EquipmentFault>>(() => [
  { title: $t('page.mes.fault.faultNo'), key: 'faultNo' },
  { title: $t('page.mes.fault.equipment'), key: 'equipmentId', render: row => equipmentName.value(row.equipmentId) },
  { title: $t('page.mes.fault.severity'), key: 'severity', width: 110 },
  { title: $t('page.mes.fault.status'), key: 'status', width: 100 },
  { title: $t('page.mes.fault.description'), key: 'description' },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 90,
    render: row =>
      row.status === 'OPEN' && hasAuth('mes:fault:close')
        ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => openClose(row) }, () => $t('page.mes.fault.close'))
        : null
  }
]);

async function fetchData() {
  loading.value = true;
  try {
    const [faultRes, equipmentPage, lotPage] = await Promise.all([
      getEquipmentFaults(),
      getEquipmentList({ page: 1, pageSize: 200 }),
      getLotList({ page: 1, pageSize: 200 })
    ]);
    rows.value = faultRes.data ?? [];
    equipment.value = equipmentPage.data?.records ?? [];
    lots.value = lotPage.data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

async function submitOpen() {
  await openRef.value?.validate();
  if (!openForm.equipmentId) return;
  await openEquipmentFault({
    faultNo: openForm.faultNo,
    equipmentId: openForm.equipmentId,
    description: openForm.description,
    severity: openForm.severity,
    lotIds: openForm.lotIds,
    downEquipment: openForm.downEquipment
  });
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
}

async function submitClose() {
  await closeRef.value?.validate();
  if (!current.value) return;
  await closeEquipmentFault(String(current.value.id), { solution: closeForm.solution });
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.fault.title') }}</span>
          <PermissionGuard code="mes:fault:add">
            <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
          </PermissionGuard>
        </div>
      </template>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showOpen" :title="$t('page.mes.fault.addTitle')" class="w-640px" :on-confirm="submitOpen">
      <div class="mb-12px">{{ $t('page.mes.fault.hint') }}</div>
      <NForm ref="openRef" :model="openForm" :rules="openRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.fault.faultNo')" path="faultNo">
          <NInput v-model:value="openForm.faultNo" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.fault.equipment')" path="equipmentId">
          <NSelect v-model:value="openForm.equipmentId" :options="equipmentOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.fault.severity')" path="severity">
          <NSelect v-model:value="openForm.severity" :options="severityOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.fault.description')" path="description">
          <NInput v-model:value="openForm.description" type="textarea" :rows="2" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.fault.lots')" path="lotIds">
          <NSelect v-model:value="openForm.lotIds" :options="lotOptions" multiple filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.fault.down')" path="downEquipment">
          <NSwitch v-model:value="openForm.downEquipment" />
        </NFormItem>
      </NForm>
    </CrudDialog>
    <CrudDialog v-model:show="showClose" :title="$t('page.mes.fault.closeTitle')" class="w-560px" :on-confirm="submitClose">
      <NForm ref="closeRef" :model="closeForm" :rules="closeRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.fault.solution')" path="solution">
          <NInput v-model:value="closeForm.solution" type="textarea" :rows="3" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
