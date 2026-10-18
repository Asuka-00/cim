<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getCountUnitList, type CountUnit } from '@/service/api/mes/count-unit';
import { getEquipmentGroupList, type EquipmentGroup } from '@/service/api/mes/equipment-group';
import { getRouteList, type Route } from '@/service/api/mes/route';
import { createRouteStep, deleteRouteStep, getRouteStepList, updateRouteStep, type RouteStep } from '@/service/api/mes/route-step';
import { getRouteVersionList, type RouteVersion } from '@/service/api/mes/route-version';
import { getStepList, type Step } from '@/service/api/mes/step';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesRouteStep' });

const ACTIONS = ['HOLD', 'WARN'];
const { hasAuth } = useAuth();
const routes = ref<Route[]>([]);
const versions = ref<RouteVersion[]>([]);
const stations = ref<Step[]>([]);
const groups = ref<EquipmentGroup[]>([]);
const units = ref<CountUnit[]>([]);
const routeId = ref<string | null>(null);
const versionId = ref<string | null>(null);
const loading = ref(false);
const rows = ref<RouteStep[]>([]);
const showModal = ref(false);
const editing = ref<RouteStep | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  seqNo: null as number | null,
  stepId: null as string | null,
  stepAlias: '',
  isRequired: true,
  equipmentGroupId: null as string | null,
  countUnitId: null as string | null,
  isUnitConvert: false,
  convertToUnitId: null as string | null,
  queueTimeMaxMin: null as number | null,
  queueTimeWarnMin: null as number | null,
  queueTimeAction: null as string | null
});

const formRules = computed<FormRules>(() => ({
  seqNo: [{ required: true, type: 'number', message: $t('page.mes.routeStep.required'), trigger: 'change' }],
  stepId: [{ required: true, message: $t('page.mes.routeStep.required'), trigger: 'change' }]
}));
const routeOptions = computed(() => routes.value.map(item => ({ label: `${item.routeCode} ${item.routeName}`, value: String(item.id) })));
const versionOptions = computed(() => versions.value.map(item => ({ label: `${item.versionNo} ${item.status}`, value: String(item.id) })));
const stationOptions = computed(() => stations.value.map(item => ({ label: `${item.stepCode} ${item.stepName}`, value: String(item.id) })));
const groupOptions = computed(() => groups.value.map(item => ({ label: `${item.groupCode} ${item.groupName}`, value: String(item.id) })));
const unitOptions = computed(() => units.value.map(item => ({ label: `${item.unitCode} ${item.unitName}`, value: String(item.id) })));
const actionOptions = ACTIONS.map(item => ({ label: item, value: item }));
const draft = computed(() => versions.value.find(item => String(item.id) === versionId.value)?.status === 'DRAFT');

function stationLabel(id?: string | null) {
  const step = stations.value.find(item => String(item.id) === String(id));
  return step ? `${step.stepCode} ${step.stepName}` : String(id ?? '');
}

function openCreate() {
  editing.value = null;
  Object.assign(form, {
    seqNo: null,
    stepId: null,
    stepAlias: '',
    isRequired: true,
    equipmentGroupId: null,
    countUnitId: null,
    isUnitConvert: false,
    convertToUnitId: null,
    queueTimeMaxMin: null,
    queueTimeWarnMin: null,
    queueTimeAction: null
  });
  showModal.value = true;
}

function openEdit(row: RouteStep) {
  editing.value = row;
  Object.assign(form, {
    seqNo: row.seqNo,
    stepId: String(row.stepId),
    stepAlias: row.stepAlias ?? '',
    isRequired: row.isRequired !== 0,
    equipmentGroupId: row.equipmentGroupId ? String(row.equipmentGroupId) : null,
    countUnitId: row.countUnitId ? String(row.countUnitId) : null,
    isUnitConvert: row.isUnitConvert === 1,
    convertToUnitId: row.convertToUnitId ? String(row.convertToUnitId) : null,
    queueTimeMaxMin: row.queueTimeMaxMin ?? null,
    queueTimeWarnMin: row.queueTimeWarnMin ?? null,
    queueTimeAction: row.queueTimeAction ?? null
  });
  showModal.value = true;
}

const columns = computed<DataTableColumns<RouteStep>>(() => [
  { title: $t('page.mes.routeStep.seqNo'), key: 'seqNo', width: 80 },
  { title: $t('page.mes.routeStep.step'), key: 'stepId', render: row => stationLabel(row.stepId) },
  { title: $t('page.mes.routeStep.alias'), key: 'stepAlias' },
  { title: $t('page.mes.routeStep.queueMax'), key: 'queueTimeMaxMin', width: 120 },
  { title: $t('page.mes.routeStep.queueAction'), key: 'queueTimeAction', width: 110 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 140,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        draft.value && hasAuth('mes:route-step:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        draft.value && hasAuth('mes:route-step:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);

async function onRouteChange(value: string | null) {
  versionId.value = null;
  versions.value = [];
  rows.value = [];
  if (!value) return;
  const page = await getRouteVersionList(value, { page: 1, pageSize: 100 });
  versions.value = page.data?.records ?? [];
}

async function fetchData() {
  if (!versionId.value) {
    rows.value = [];
    return;
  }
  loading.value = true;
  try {
    const res = await getRouteStepList(versionId.value);
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function submit() {
  await formRef.value?.validate();
  if (!versionId.value || !draft.value) return false;
  if (form.isUnitConvert && !form.convertToUnitId) {
    window.$message?.warning($t('page.mes.routeStep.convertRequired'));
    return false;
  }
  const data = {
    seqNo: form.seqNo,
    stepId: form.stepId,
    stepAlias: form.stepAlias,
    isRequired: form.isRequired ? 1 : 0,
    equipmentGroupId: form.equipmentGroupId,
    countUnitId: form.countUnitId,
    isUnitConvert: form.isUnitConvert ? 1 : 0,
    convertToUnitId: form.isUnitConvert ? form.convertToUnitId : null,
    queueTimeMaxMin: form.queueTimeMaxMin,
    queueTimeWarnMin: form.queueTimeWarnMin,
    queueTimeAction: form.queueTimeAction
  };
  if (editing.value) await updateRouteStep(versionId.value, String(editing.value.id), data);
  else await createRouteStep(versionId.value, data);
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
  return true;
}

function handleDelete(row: RouteStep) {
  if (!versionId.value) return;
  const version = versionId.value;
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.routeStep.deleteConfirm', { name: String(row.seqNo) }),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteRouteStep(version, String(row.id));
      window.$message?.success($t('common.saveSuccess'));
      await fetchData();
    }
  });
}

onMounted(async () => {
  const [routePage, stepPage, groupPage, unitPage] = await Promise.all([
    getRouteList({ page: 1, pageSize: 200 }),
    getStepList({ page: 1, pageSize: 200 }),
    getEquipmentGroupList({ page: 1, pageSize: 200 }),
    getCountUnitList({ page: 1, pageSize: 200 })
  ]);
  routes.value = routePage.data?.records ?? [];
  stations.value = stepPage.data?.records ?? [];
  groups.value = groupPage.data?.records ?? [];
  units.value = unitPage.data?.records ?? [];
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.routeStep.title') }}</span>
          <div class="flex items-center gap-8px">
            <NSelect v-model:value="routeId" class="w-220px" :options="routeOptions" filterable @update:value="onRouteChange" />
            <NSelect v-model:value="versionId" class="w-160px" :options="versionOptions" :disabled="!routeId" @update:value="fetchData" />
            <PermissionGuard code="mes:route-step:add">
              <NButton type="primary" :disabled="!versionId || !draft" @click="openCreate">{{ $t('common.add') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.routeStep.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog
      v-model:show="showModal"
      :title="editing ? $t('page.mes.routeStep.editTitle') : $t('page.mes.routeStep.addTitle')"
      class="w-640px"
      :on-confirm="submit"
    >
      <NForm ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="120">
        <NFormItem :label="$t('page.mes.routeStep.seqNo')" path="seqNo">
          <NInputNumber v-model:value="form.seqNo" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeStep.step')" path="stepId">
          <NSelect v-model:value="form.stepId" :options="stationOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeStep.alias')" path="stepAlias">
          <NInput v-model:value="form.stepAlias" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeStep.isRequired')" path="isRequired">
          <NSwitch v-model:value="form.isRequired" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeStep.equipmentGroup')" path="equipmentGroupId">
          <NSelect v-model:value="form.equipmentGroupId" :options="groupOptions" filterable clearable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeStep.countUnit')" path="countUnitId">
          <NSelect v-model:value="form.countUnitId" :options="unitOptions" filterable clearable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeStep.queueMax')" path="queueTimeMaxMin">
          <NInputNumber v-model:value="form.queueTimeMaxMin" :min="1" clearable class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeStep.queueWarn')" path="queueTimeWarnMin">
          <NInputNumber v-model:value="form.queueTimeWarnMin" :min="1" clearable class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeStep.queueAction')" path="queueTimeAction">
          <NSelect v-model:value="form.queueTimeAction" :options="actionOptions" clearable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeStep.unitConvert')" path="isUnitConvert">
          <NSwitch v-model:value="form.isUnitConvert" />
        </NFormItem>
        <NFormItem v-if="form.isUnitConvert" :label="$t('page.mes.routeStep.convertUnit')" path="convertToUnitId">
          <NSelect v-model:value="form.convertToUnitId" :options="unitOptions" filterable />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
