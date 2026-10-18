<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getBomList, type Bom } from '@/service/api/mes/bom';
import { createBomItem, deleteBomItem, getBomItemList, updateBomItem, type BomItem } from '@/service/api/mes/bom-item';
import { getBomVersionList, type BomVersion } from '@/service/api/mes/bom-version';
import { getMaterialList, type Material } from '@/service/api/mes/material';
import { getStepList, type Step } from '@/service/api/mes/step';
import { getUomList, type Uom } from '@/service/api/mes/uom';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesBomItem' });

const MODES = ['PER_WAFER', 'PER_DIE', 'PER_STRIP', 'PER_UNIT', 'PER_LOT', 'PER_RUN', 'PER_TIME'];
const POLICIES = ['ALLOW', 'WARN', 'BLOCK'];
const { hasAuth } = useAuth();
const boms = ref<Bom[]>([]);
const versions = ref<BomVersion[]>([]);
const materials = ref<Material[]>([]);
const uoms = ref<Uom[]>([]);
const steps = ref<Step[]>([]);
const bomId = ref<string | null>(null);
const versionId = ref<string | null>(null);
const loading = ref(false);
const rows = ref<BomItem[]>([]);
const showModal = ref(false);
const editing = ref<BomItem | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  lineNo: null as number | null,
  materialId: null as string | null,
  qtyPer: null as number | null,
  uomId: null as string | null,
  consumeMode: 'PER_WAFER',
  stepId: null as string | null,
  routeSeqNo: 0 as number | null,
  backflush: true,
  substituteGroup: '',
  substitutePolicy: 'ALLOW'
});

const formRules = computed<FormRules>(() => ({
  lineNo: [{ required: true, type: 'number', message: $t('page.mes.bomItem.required'), trigger: 'change' }],
  materialId: [{ required: true, message: $t('page.mes.bomItem.required'), trigger: 'change' }],
  qtyPer: [{ required: true, type: 'number', message: $t('page.mes.bomItem.required'), trigger: 'change' }],
  uomId: [{ required: true, message: $t('page.mes.bomItem.required'), trigger: 'change' }],
  stepId: [{ required: true, message: $t('page.mes.bomItem.required'), trigger: 'change' }]
}));
const bomOptions = computed(() => boms.value.map(item => ({ label: `${item.bomCode} ${item.bomName}`, value: String(item.id) })));
const versionOptions = computed(() => versions.value.map(item => ({ label: `${item.versionNo} ${item.status}`, value: String(item.id) })));
const materialOptions = computed(() => materials.value.map(item => ({ label: `${item.materialCode} ${item.materialName}`, value: String(item.id) })));
const uomOptions = computed(() => uoms.value.map(item => ({ label: `${item.uomCode} ${item.uomName}`, value: String(item.id) })));
const stepOptions = computed(() => steps.value.map(item => ({ label: `${item.stepCode} ${item.stepName}`, value: String(item.id) })));
const modeOptions = MODES.map(item => ({ label: item, value: item }));
const policyOptions = POLICIES.map(item => ({ label: item, value: item }));
const draft = computed(() => versions.value.find(item => String(item.id) === versionId.value)?.status === 'DRAFT');

function materialLabel(id?: string | null) {
  const material = materials.value.find(item => String(item.id) === String(id));
  return material ? `${material.materialCode} ${material.materialName}` : String(id ?? '');
}

function stepLabel(id?: string | null) {
  const step = steps.value.find(item => String(item.id) === String(id));
  return step ? `${step.stepCode} ${step.stepName}` : String(id ?? '');
}

function openCreate() {
  editing.value = null;
  Object.assign(form, {
    lineNo: null,
    materialId: null,
    qtyPer: null,
    uomId: null,
    consumeMode: 'PER_WAFER',
    stepId: null,
    routeSeqNo: 0,
    backflush: true,
    substituteGroup: '',
    substitutePolicy: 'ALLOW'
  });
  showModal.value = true;
}

function openEdit(row: BomItem) {
  editing.value = row;
  Object.assign(form, {
    lineNo: row.lineNo,
    materialId: String(row.materialId),
    qtyPer: row.qtyPer,
    uomId: String(row.uomId),
    consumeMode: row.consumeMode || 'PER_WAFER',
    stepId: String(row.stepId),
    routeSeqNo: row.routeSeqNo ?? 0,
    backflush: row.backflush !== 0,
    substituteGroup: row.substituteGroup ?? '',
    substitutePolicy: row.substitutePolicy || 'ALLOW'
  });
  showModal.value = true;
}

const columns = computed<DataTableColumns<BomItem>>(() => [
  { title: $t('page.mes.bomItem.lineNo'), key: 'lineNo', width: 80 },
  { title: $t('page.mes.bomItem.material'), key: 'materialId', render: row => materialLabel(row.materialId) },
  { title: $t('page.mes.bomItem.qtyPer'), key: 'qtyPer', width: 100 },
  { title: $t('page.mes.bomItem.consumeMode'), key: 'consumeMode', width: 130 },
  { title: $t('page.mes.bomItem.step'), key: 'stepId', render: row => stepLabel(row.stepId) },
  { title: $t('page.mes.bomItem.routeSeq'), key: 'routeSeqNo', width: 100 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 140,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        draft.value && hasAuth('mes:bom-item:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        draft.value && hasAuth('mes:bom-item:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);

async function onBomChange(value: string | null) {
  versionId.value = null;
  versions.value = [];
  rows.value = [];
  if (!value) return;
  const page = await getBomVersionList(value, { page: 1, pageSize: 100 });
  versions.value = page.data?.records ?? [];
}

async function fetchData() {
  if (!versionId.value) {
    rows.value = [];
    return;
  }
  loading.value = true;
  try {
    const res = await getBomItemList(versionId.value);
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function submit() {
  await formRef.value?.validate();
  if (!versionId.value || !draft.value || form.qtyPer == null || form.qtyPer <= 0) return false;
  if (form.routeSeqNo != null && form.routeSeqNo < 0) {
    window.$message?.warning($t('page.mes.bomItem.seqInvalid'));
    return false;
  }
  const data = {
    lineNo: form.lineNo,
    materialId: form.materialId,
    qtyPer: form.qtyPer,
    uomId: form.uomId,
    consumeMode: form.consumeMode,
    stepId: form.stepId,
    routeSeqNo: form.routeSeqNo ?? 0,
    backflush: form.backflush ? 1 : 0,
    substituteGroup: form.substituteGroup,
    substitutePolicy: form.substitutePolicy
  };
  if (editing.value) await updateBomItem(versionId.value, String(editing.value.id), data);
  else await createBomItem(versionId.value, data);
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
  return true;
}

function handleDelete(row: BomItem) {
  if (!versionId.value) return;
  const version = versionId.value;
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.bomItem.deleteConfirm', { name: String(row.lineNo) }),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteBomItem(version, String(row.id));
      window.$message?.success($t('common.saveSuccess'));
      await fetchData();
    }
  });
}

onMounted(async () => {
  const [bomPage, materialPage, uomPage, stepPage] = await Promise.all([
    getBomList({ page: 1, pageSize: 200 }),
    getMaterialList({ page: 1, pageSize: 200 }),
    getUomList({ page: 1, pageSize: 200 }),
    getStepList({ page: 1, pageSize: 200 })
  ]);
  boms.value = bomPage.data?.records ?? [];
  materials.value = materialPage.data?.records ?? [];
  uoms.value = uomPage.data?.records ?? [];
  steps.value = stepPage.data?.records ?? [];
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.bomItem.title') }}</span>
          <div class="flex items-center gap-8px">
            <NSelect v-model:value="bomId" class="w-240px" :options="bomOptions" filterable @update:value="onBomChange" />
            <NSelect v-model:value="versionId" class="w-180px" :options="versionOptions" :disabled="!bomId" @update:value="fetchData" />
            <PermissionGuard code="mes:bom-item:add">
              <NButton type="primary" :disabled="!versionId || !draft" @click="openCreate">{{ $t('common.add') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.bomItem.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog
      v-model:show="showModal"
      :title="editing ? $t('page.mes.bomItem.editTitle') : $t('page.mes.bomItem.addTitle')"
      class="w-640px"
      :on-confirm="submit"
    >
      <NForm ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="110">
        <NFormItem :label="$t('page.mes.bomItem.lineNo')" path="lineNo">
          <NInputNumber v-model:value="form.lineNo" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.bomItem.material')" path="materialId">
          <NSelect v-model:value="form.materialId" :options="materialOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.bomItem.qtyPer')" path="qtyPer">
          <NInputNumber v-model:value="form.qtyPer" :min="0.000001" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.bomItem.uom')" path="uomId">
          <NSelect v-model:value="form.uomId" :options="uomOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.bomItem.step')" path="stepId">
          <NSelect v-model:value="form.stepId" :options="stepOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.bomItem.routeSeq')" path="routeSeqNo">
          <NInputNumber v-model:value="form.routeSeqNo" :min="0" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.bomItem.consumeMode')" path="consumeMode">
          <NSelect v-model:value="form.consumeMode" :options="modeOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.bomItem.backflush')" path="backflush">
          <NSwitch v-model:value="form.backflush" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.bomItem.substituteGroup')" path="substituteGroup">
          <NInput v-model:value="form.substituteGroup" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.bomItem.substitutePolicy')" path="substitutePolicy">
          <NSelect v-model:value="form.substitutePolicy" :options="policyOptions" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
