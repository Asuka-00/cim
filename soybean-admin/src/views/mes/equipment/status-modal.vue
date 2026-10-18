<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import type { FormInst, FormRules } from 'naive-ui';
import {
  changeEquipmentStatus,
  getEquipmentStatusHistory,
  type EquipmentStatusHist
} from '@/service/api/mes/equipment';
import { $t } from '@/locales';

const props = defineProps<{
  show: boolean;
  equipmentId: string;
  equipmentCode: string;
  e10State: string;
}>();

const emit = defineEmits<{
  'update:show': [value: boolean];
  changed: [];
}>();

const DOWN = new Set(['SCHEDULED_DOWN', 'UNSCHEDULED_DOWN']);
const e10Options = ['PRODUCTIVE', 'STANDBY', 'ENGINEERING', 'SCHEDULED_DOWN', 'UNSCHEDULED_DOWN', 'NON_SCHEDULED'].map(
  value => ({ label: value, value })
);

const visible = computed({
  get: () => props.show,
  set: value => emit('update:show', value)
});

const loading = ref(false);
const currentState = ref(props.e10State);
const history = ref<EquipmentStatusHist[]>([]);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  e10State: 'STANDBY',
  statusCode: '',
  reasonCode: '',
  remark: ''
});

const rules = computed<FormRules>(() => ({
  e10State: [{ required: true, message: $t('page.mes.equipment.required'), trigger: 'change' }],
  reasonCode: DOWN.has(form.e10State)
    ? [{ required: true, message: $t('page.mes.equipment.reasonRequired'), trigger: 'blur' }]
    : []
}));

async function load() {
  loading.value = true;
  try {
    const { data } = await getEquipmentStatusHistory(props.equipmentId, { page: 1, pageSize: 20 });
    history.value = data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

watch(
  () => [props.show, props.equipmentId] as const,
  ([show]) => {
    if (show) {
      currentState.value = props.e10State;
      Object.assign(form, { e10State: props.e10State || 'STANDBY', statusCode: '', reasonCode: '', remark: '' });
      load();
    }
  }
);

async function submit() {
  await formRef.value?.validate();
  await changeEquipmentStatus(props.equipmentId, { ...form });
  currentState.value = form.e10State;
  window.$message?.success($t('common.saveSuccess'));
  emit('changed');
  await load();
}
</script>

<template>
  <NModal v-model:show="visible" preset="card" class="w-760px" :title="`${$t('page.mes.equipment.statusTitle')} ${equipmentCode}`">
    <div class="mb-12px">{{ $t('page.mes.equipment.currentState') }}：{{ currentState }}</div>
    <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
      <NFormItem :label="$t('page.mes.equipment.e10State')" path="e10State">
        <NSelect v-model:value="form.e10State" :options="e10Options" />
      </NFormItem>
      <NFormItem :label="$t('page.mes.equipment.statusCode')" path="statusCode">
        <NInput v-model:value="form.statusCode" />
      </NFormItem>
      <NFormItem :label="$t('page.mes.equipment.reasonCode')" path="reasonCode">
        <NInput v-model:value="form.reasonCode" />
      </NFormItem>
      <NFormItem :label="$t('common.remark')" path="remark">
        <NInput v-model:value="form.remark" />
      </NFormItem>
      <div class="mb-16px flex justify-end">
        <NButton type="primary" @click="submit">{{ $t('page.mes.equipment.changeStatus') }}</NButton>
      </div>
    </NForm>
    <NDataTable
      :loading="loading"
      :bordered="false"
      size="small"
      :data="history"
      :columns="[
        { title: $t('page.mes.equipment.fromState'), key: 'fromE10State' },
        { title: $t('page.mes.equipment.toState'), key: 'toE10State' },
        { title: $t('page.mes.equipment.startAt'), key: 'startAt' },
        { title: $t('page.mes.equipment.endAt'), key: 'endAt' },
        { title: $t('page.mes.equipment.durationSec'), key: 'durationSec', width: 90 }
      ]"
    />
  </NModal>
</template>
