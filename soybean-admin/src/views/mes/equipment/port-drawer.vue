<script setup lang="ts">
import { computed, h, reactive, ref, watch } from 'vue';
import { NButton, type DataTableColumns, type SelectOption } from 'naive-ui';
import {
  changeChamberStatus,
  createEquipmentPort,
  deleteEquipmentPort,
  getEquipmentPortList,
  updateEquipmentPort,
  type EquipmentPort
} from '@/service/api/mes/equipment-port';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

const props = defineProps<{
  show: boolean;
  equipmentId: string;
  equipmentCode: string;
}>();

const emit = defineEmits<{ 'update:show': [value: boolean] }>();

const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<EquipmentPort[]>([]);
const editing = ref<EquipmentPort | null>(null);
const statusTarget = ref<EquipmentPort | null>(null);
const form = reactive({ portCode: '', portName: '', kind: 'PORT', maxConcurrent: 1 as number | null, enabled: 1 });
const statusForm = reactive({ e10State: 'STANDBY', reasonCode: '' });
const kindOptions: SelectOption[] = [
  { label: 'PORT', value: 'PORT' },
  { label: 'CHAMBER', value: 'CHAMBER' }
];
const e10Options = ['PRODUCTIVE', 'STANDBY', 'ENGINEERING', 'SCHEDULED_DOWN', 'UNSCHEDULED_DOWN', 'NON_SCHEDULED'].map(
  value => ({ label: value, value })
);

const visible = computed({
  get: () => props.show,
  set: value => emit('update:show', value)
});

function resetForm() {
  editing.value = null;
  Object.assign(form, { portCode: '', portName: '', kind: 'PORT', maxConcurrent: 1, enabled: 1 });
}

async function load() {
  if (!props.equipmentId) return;
  loading.value = true;
  try {
    const { data } = await getEquipmentPortList(props.equipmentId);
    rows.value = data ?? [];
  } finally {
    loading.value = false;
  }
}

watch(
  () => [props.show, props.equipmentId] as const,
  ([show]) => {
    if (show) {
      resetForm();
      statusTarget.value = null;
      load();
    }
  }
);

function editRow(row: EquipmentPort) {
  editing.value = row;
  Object.assign(form, {
    portCode: row.portCode,
    portName: row.portName ?? '',
    kind: row.kind,
    maxConcurrent: row.maxConcurrent,
    enabled: row.enabled
  });
}

async function save() {
  if (!form.portCode.trim() || form.maxConcurrent == null || form.maxConcurrent <= 0) {
    window.$message?.warning($t('page.mes.equipmentPort.required'));
    return;
  }
  if (editing.value) {
    await updateEquipmentPort(props.equipmentId, editing.value.id, form);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createEquipmentPort(props.equipmentId, form);
    window.$message?.success($t('common.addSuccess'));
  }
  resetForm();
  await load();
}

function remove(row: EquipmentPort) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.equipmentPort.deleteConfirm', { name: row.portCode }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteEquipmentPort(props.equipmentId, row.id);
      window.$message?.success($t('common.deleteSuccess'));
      await load();
    }
  });
}

async function changeStatus() {
  if (!statusTarget.value) return;
  const down = statusForm.e10State === 'SCHEDULED_DOWN' || statusForm.e10State === 'UNSCHEDULED_DOWN';
  if (down && !statusForm.reasonCode.trim()) {
    window.$message?.warning($t('page.mes.equipmentPort.reasonRequired'));
    return;
  }
  await changeChamberStatus(props.equipmentId, statusTarget.value.id, {
    e10State: statusForm.e10State,
    reasonCode: statusForm.reasonCode.trim()
  });
  window.$message?.success($t('page.mes.equipmentPort.statusChanged'));
  statusTarget.value = null;
  await load();
}

const columns = computed<DataTableColumns<EquipmentPort>>(() => [
  { title: $t('page.mes.equipmentPort.portCode'), key: 'portCode' },
  { title: $t('page.mes.equipmentPort.kind'), key: 'kind', width: 110 },
  { title: $t('page.mes.equipment.e10State'), key: 'e10State', width: 160 },
  { title: $t('page.mes.equipmentPort.maxConcurrent'), key: 'maxConcurrent', width: 90 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 220,
    render: row =>
      h('div', { class: 'flex gap-8px flex-wrap' }, [
        row.kind === 'CHAMBER' && hasAuth('mes:equipment:edit')
          ? h(
              NButton,
              {
                size: 'small',
                quaternary: true,
                onClick: () => {
                  statusTarget.value = row;
                  statusForm.e10State = row.e10State;
                  statusForm.reasonCode = '';
                }
              },
              () => $t('page.mes.equipment.changeStatus')
            )
          : null,
        hasAuth('mes:equipment:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => editRow(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:equipment:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => remove(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);
</script>

<template>
  <NModal v-model:show="visible" preset="card" class="w-860px" :title="`${$t('page.mes.equipmentPort.title')} ${equipmentCode}`">
    <DataTable :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    <NForm v-if="statusTarget" :model="statusForm" label-placement="left" label-width="96" class="mt-16px">
      <NFormItem :label="$t('page.mes.equipment.e10State')">
        <NSelect v-model:value="statusForm.e10State" :options="e10Options" />
      </NFormItem>
      <NFormItem :label="$t('page.mes.equipmentPort.reasonCode')">
        <NInput v-model:value="statusForm.reasonCode" />
      </NFormItem>
      <div class="flex justify-end">
        <NButton type="primary" @click="changeStatus">{{ $t('page.mes.equipment.changeStatus') }}</NButton>
      </div>
    </NForm>
    <NForm :model="form" label-placement="left" label-width="96" class="mt-16px">
      <NFormItem :label="$t('page.mes.equipmentPort.portCode')">
        <NInput v-model:value="form.portCode" />
      </NFormItem>
      <NFormItem :label="$t('page.mes.equipmentPort.portName')">
        <NInput v-model:value="form.portName" />
      </NFormItem>
      <NFormItem :label="$t('page.mes.equipmentPort.kind')">
        <NSelect v-model:value="form.kind" :options="kindOptions" />
      </NFormItem>
      <NFormItem :label="$t('page.mes.equipmentPort.maxConcurrent')">
        <NInputNumber v-model:value="form.maxConcurrent" :min="1" class="w-full" />
      </NFormItem>
      <NFormItem :label="$t('common.status')">
        <NRadioGroup v-model:value="form.enabled">
          <NRadio :value="1">{{ $t('common.enabled') }}</NRadio>
          <NRadio :value="0">{{ $t('common.disabled') }}</NRadio>
        </NRadioGroup>
      </NFormItem>
      <div class="flex justify-end gap-8px">
        <NButton @click="resetForm">{{ $t('common.reset') }}</NButton>
        <NButton v-if="!editing && hasAuth('mes:equipment:add')" type="primary" @click="save">{{ $t('common.add') }}</NButton>
        <NButton v-else-if="editing && hasAuth('mes:equipment:edit')" type="primary" @click="save">{{ $t('common.save') }}</NButton>
      </div>
    </NForm>
  </NModal>
</template>
