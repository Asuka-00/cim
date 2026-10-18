<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import {
  createEquipmentPort,
  deleteEquipmentPort,
  getEquipmentPorts,
  updateEquipmentPort,
  type EquipmentPort
} from '@/service/api/mes/equipment-port';
import { getEquipmentList, type Equipment } from '@/service/api/mes/equipment';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesEquipmentPort' });

const KINDS = ['PORT', 'CHAMBER'];
const { hasAuth } = useAuth();
const equipment = ref<Equipment[]>([]);
const equipmentId = ref<string | null>(null);
const loading = ref(false);
const rows = ref<EquipmentPort[]>([]);
const showModal = ref(false);
const editing = ref<EquipmentPort | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({ portCode: '', portName: '', kind: 'PORT', maxConcurrent: 1, enabled: true });

const rules = computed<FormRules>(() => ({
  portCode: [{ required: true, message: $t('page.mes.port.required'), trigger: 'blur' }]
}));
const equipmentOptions = computed(() =>
  equipment.value.map(item => ({ label: `${item.equipmentCode} ${item.equipmentName}`, value: String(item.id) }))
);
const kindOptions = KINDS.map(item => ({ label: item, value: item }));

function openCreate() {
  editing.value = null;
  Object.assign(form, { portCode: '', portName: '', kind: 'PORT', maxConcurrent: 1, enabled: true });
  showModal.value = true;
}

function openEdit(row: EquipmentPort) {
  editing.value = row;
  Object.assign(form, {
    portCode: row.portCode,
    portName: row.portName ?? '',
    kind: row.kind || 'PORT',
    maxConcurrent: row.maxConcurrent ?? 1,
    enabled: row.enabled !== 0
  });
  showModal.value = true;
}

const columns = computed<DataTableColumns<EquipmentPort>>(() => [
  { title: $t('page.mes.port.code'), key: 'portCode', width: 140 },
  { title: $t('page.mes.port.name'), key: 'portName' },
  { title: $t('page.mes.port.kind'), key: 'kind', width: 110 },
  { title: $t('page.mes.port.capacity'), key: 'maxConcurrent', width: 110 },
  { title: $t('page.mes.port.e10'), key: 'e10State', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 140,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        hasAuth('mes:port:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:port:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);

async function fetchData() {
  if (!equipmentId.value) {
    rows.value = [];
    return;
  }
  loading.value = true;
  try {
    const res = await getEquipmentPorts(equipmentId.value);
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function submit() {
  await formRef.value?.validate();
  if (!equipmentId.value) return;
  const data = {
    portCode: form.portCode,
    portName: form.portName,
    kind: form.kind,
    maxConcurrent: form.maxConcurrent,
    enabled: form.enabled ? 1 : 0
  };
  if (editing.value) await updateEquipmentPort(equipmentId.value, String(editing.value.id), data);
  else await createEquipmentPort(equipmentId.value, data);
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

function handleDelete(row: EquipmentPort) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.port.deleteConfirm', { name: row.portCode }),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      if (!equipmentId.value) return;
      await deleteEquipmentPort(equipmentId.value, String(row.id));
      window.$message?.success($t('common.saveSuccess'));
      await fetchData();
    }
  });
}

onMounted(async () => {
  const page = await getEquipmentList({ page: 1, pageSize: 200 });
  equipment.value = page.data?.records ?? [];
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.port.title') }}</span>
          <div class="flex items-center gap-8px">
            <NSelect v-model:value="equipmentId" class="w-260px" :options="equipmentOptions" filterable @update:value="fetchData" />
            <PermissionGuard code="mes:port:add">
              <NButton type="primary" :disabled="!equipmentId" @click="openCreate">{{ $t('common.add') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.port.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showModal" :title="editing ? $t('page.mes.port.editTitle') : $t('page.mes.port.addTitle')" class="w-560px" :on-confirm="submit">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="110">
        <NFormItem :label="$t('page.mes.port.code')" path="portCode">
          <NInput v-model:value="form.portCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.port.name')" path="portName">
          <NInput v-model:value="form.portName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.port.kind')" path="kind">
          <NSelect v-model:value="form.kind" :options="kindOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.port.capacity')" path="maxConcurrent">
          <NInputNumber v-model:value="form.maxConcurrent" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.port.enabled')" path="enabled">
          <NSwitch v-model:value="form.enabled" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
