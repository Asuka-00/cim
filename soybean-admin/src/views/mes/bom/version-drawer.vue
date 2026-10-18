<script setup lang="ts">
import { computed, h, reactive, ref, watch } from 'vue';
import { NButton, type DataTableColumns, type SelectOption } from 'naive-ui';
import { createBomItem, deleteBomItem, getBomItemList, updateBomItem, type BomItem } from '@/service/api/mes/bom-item';
import { getMaterialList, type Material } from '@/service/api/mes/material';
import { getStepList, type Step } from '@/service/api/mes/step';
import { getUomList, type Uom } from '@/service/api/mes/uom';
import {
  approveBomVersion,
  copyBomVersion,
  createBomVersion,
  getBomVersionList,
  rejectBomVersion,
  submitBomVersion,
  updateBomVersion,
  type BomVersion
} from '@/service/api/mes/bom-version';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

const props = defineProps<{
  show: boolean;
  bomId: string;
  bomCode: string;
}>();

const emit = defineEmits<{ 'update:show': [value: boolean] }>();

const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<BomVersion[]>([]);
const editing = ref<BomVersion | null>(null);
const form = reactive({ remark: '', actor: '' });
const selected = ref<BomVersion | null>(null);
const items = ref<BomItem[]>([]);
const materials = ref<Material[]>([]);
const uoms = ref<Uom[]>([]);
const stations = ref<Step[]>([]);
const editingItem = ref<BomItem | null>(null);
const itemForm = reactive({
  lineNo: null as number | null,
  materialId: null as string | null,
  qtyPer: null as number | null,
  uomId: null as string | null,
  consumeMode: 'PER_WAFER',
  stepId: null as string | null,
  routeSeqNo: 0,
  backflush: 1,
  substituteGroup: '',
  substitutePolicy: 'ALLOW'
});
const materialOptions = computed<SelectOption[]>(() => materials.value.map(item => ({ label: item.materialCode, value: item.id })));
const uomOptions = computed<SelectOption[]>(() => uoms.value.map(item => ({ label: item.uomCode, value: item.id })));
const stationOptions = computed<SelectOption[]>(() => stations.value.map(item => ({ label: item.stepCode, value: item.id })));
const modeOptions = ['PER_WAFER', 'PER_DIE', 'PER_STRIP', 'PER_UNIT', 'PER_LOT', 'PER_RUN', 'PER_TIME'].map(value => ({
  label: value,
  value
}));
const policyOptions = ['ALLOW', 'WARN', 'BLOCK'].map(value => ({ label: value, value }));
const draftSelected = computed(() => selected.value?.status === 'DRAFT');

const visible = computed({
  get: () => props.show,
  set: value => emit('update:show', value)
});

function resetForm() {
  editing.value = null;
  form.remark = '';
}

function resetItemForm() {
  editingItem.value = null;
  Object.assign(itemForm, {
    lineNo: null,
    materialId: null,
    qtyPer: null,
    uomId: null,
    consumeMode: 'PER_WAFER',
    stepId: null,
    routeSeqNo: 0,
    backflush: 1,
    substituteGroup: '',
    substitutePolicy: 'ALLOW'
  });
}

async function loadItems(version: BomVersion) {
  selected.value = version;
  const { data } = await getBomItemList(version.id);
  items.value = data ?? [];
}

async function loadMasters() {
  const [materialPage, uomPage, stepPage] = await Promise.all([
    getMaterialList({ page: 1, pageSize: 200 }),
    getUomList({ page: 1, pageSize: 200 }),
    getStepList({ page: 1, pageSize: 200 })
  ]);
  materials.value = materialPage.data?.records ?? [];
  uoms.value = uomPage.data?.records ?? [];
  stations.value = stepPage.data?.records ?? [];
}

async function load() {
  if (!props.bomId) return;
  loading.value = true;
  try {
    const { data } = await getBomVersionList(props.bomId, { page: 1, pageSize: 50 });
    rows.value = data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

watch(
  () => [props.show, props.bomId] as const,
  ([show]) => {
    if (show) {
      resetForm();
      resetItemForm();
      selected.value = null;
      items.value = [];
      load();
      loadMasters();
    }
  }
);

function editRow(row: BomVersion) {
  editing.value = row;
  form.remark = row.remark ?? '';
}

function requireActor() {
  if (!form.actor.trim()) {
    window.$message?.warning($t('page.mes.bomVersion.actorRequired'));
    return false;
  }
  return true;
}

async function save() {
  if (editing.value) {
    await updateBomVersion(props.bomId, editing.value.id, { remark: form.remark });
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createBomVersion(props.bomId, { remark: form.remark });
    window.$message?.success($t('common.addSuccess'));
  }
  resetForm();
  await load();
}

async function copy(row: BomVersion) {
  await copyBomVersion(props.bomId, row.id);
  window.$message?.success($t('common.addSuccess'));
  await load();
}

async function submit(row: BomVersion) {
  if (!requireActor()) return;
  await submitBomVersion(props.bomId, row.id, { assignee: form.actor.trim() });
  window.$message?.success($t('page.mes.bomVersion.submitted'));
  await load();
}

async function approve(row: BomVersion) {
  if (!requireActor()) return;
  await approveBomVersion(props.bomId, row.id, { operator: form.actor.trim() });
  window.$message?.success($t('page.mes.bomVersion.approved'));
  await load();
}

async function reject(row: BomVersion) {
  if (!requireActor()) return;
  await rejectBomVersion(props.bomId, row.id, { operator: form.actor.trim() });
  window.$message?.success($t('page.mes.bomVersion.rejected'));
  await load();
}

const columns = computed<DataTableColumns<BomVersion>>(() => [
  { title: $t('page.mes.bomVersion.versionNo'), key: 'versionNo', width: 80 },
  { title: $t('page.mes.bomVersion.status'), key: 'status', width: 110 },
  { title: $t('common.remark'), key: 'remark' },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 280,
    render: row =>
      h('div', { class: 'flex gap-8px flex-wrap' }, [
        h(NButton, { size: 'small', quaternary: true, onClick: () => loadItems(row) }, () => $t('page.mes.bomItem.title')),
        hasAuth('mes:bom:edit')
          ? h(NButton, { size: 'small', quaternary: true, onClick: () => copy(row) }, () => $t('page.mes.bomVersion.copy'))
          : null,
        row.status === 'DRAFT' && hasAuth('mes:bom:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => editRow(row) }, () => $t('common.edit'))
          : null,
        row.status === 'DRAFT' && hasAuth('mes:bom:edit')
          ? h(NButton, { size: 'small', quaternary: true, onClick: () => submit(row) }, () => $t('page.mes.bomVersion.submit'))
          : null,
        row.status === 'PENDING' && hasAuth('mes:bom:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'success', onClick: () => approve(row) }, () =>
              $t('page.mes.bomVersion.approve')
            )
          : null,
        row.status === 'PENDING' && hasAuth('mes:bom:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => reject(row) }, () =>
              $t('page.mes.bomVersion.reject')
            )
          : null
      ])
  }
]);

function labelOf(options: SelectOption[], id?: string | null) {
  return String(options.find(item => item.value === id)?.label ?? '');
}

function editItem(row: BomItem) {
  editingItem.value = row;
  Object.assign(itemForm, {
    lineNo: row.lineNo,
    materialId: row.materialId,
    qtyPer: row.qtyPer,
    uomId: row.uomId,
    consumeMode: row.consumeMode,
    stepId: row.stepId,
    routeSeqNo: row.routeSeqNo,
    backflush: row.backflush,
    substituteGroup: row.substituteGroup ?? '',
    substitutePolicy: row.substitutePolicy ?? 'ALLOW'
  });
}

async function saveItem() {
  if (!selected.value || itemForm.lineNo == null || !itemForm.materialId || itemForm.qtyPer == null || !itemForm.uomId || !itemForm.stepId) {
    window.$message?.warning($t('page.mes.bomItem.required'));
    return;
  }
  if (editingItem.value) {
    await updateBomItem(selected.value.id, editingItem.value.id, itemForm);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createBomItem(selected.value.id, itemForm);
    window.$message?.success($t('common.addSuccess'));
  }
  resetItemForm();
  await loadItems(selected.value);
}

function removeItem(row: BomItem) {
  if (!selected.value) return;
  const version = selected.value;
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.bomItem.deleteConfirm', { name: String(row.lineNo) }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteBomItem(version.id, row.id);
      window.$message?.success($t('common.deleteSuccess'));
      await loadItems(version);
    }
  });
}

const itemColumns = computed<DataTableColumns<BomItem>>(() => [
  { title: $t('page.mes.bomItem.lineNo'), key: 'lineNo', width: 70 },
  { title: $t('page.mes.bomItem.material'), key: 'materialId', render: row => labelOf(materialOptions.value, row.materialId) },
  { title: $t('page.mes.bomItem.qtyPer'), key: 'qtyPer', width: 90 },
  { title: $t('page.mes.bomItem.step'), key: 'stepId', render: row => labelOf(stationOptions.value, row.stepId) },
  { title: $t('page.mes.bomItem.routeSeq'), key: 'routeSeqNo', width: 90 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 140,
    render: row =>
      draftSelected.value
        ? h('div', { class: 'flex gap-8px' }, [
            hasAuth('mes:bom:edit')
              ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => editItem(row) }, () => $t('common.edit'))
              : null,
            hasAuth('mes:bom:delete')
              ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => removeItem(row) }, () => $t('common.delete'))
              : null
          ])
        : $t('page.mes.bomItem.readonly')
  }
]);
</script>

<template>
  <NModal v-model:show="visible" preset="card" class="w-960px" :title="`${$t('page.mes.bomVersion.title')} ${bomCode}`">
    <DataTable :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    <NForm :model="form" label-placement="left" label-width="96" class="mt-16px">
      <NFormItem :label="$t('common.remark')">
        <NInput v-model:value="form.remark" />
      </NFormItem>
      <NFormItem :label="$t('page.mes.bomVersion.actor')">
        <NInput v-model:value="form.actor" :placeholder="$t('page.mes.bomVersion.actorHint')" />
      </NFormItem>
      <div class="flex justify-end gap-8px">
        <NButton @click="resetForm">{{ $t('common.reset') }}</NButton>
        <NButton v-if="!editing && hasAuth('mes:bom:add')" type="primary" @click="save">{{ $t('common.add') }}</NButton>
        <NButton v-else-if="editing && hasAuth('mes:bom:edit')" type="primary" @click="save">{{ $t('common.save') }}</NButton>
      </div>
    </NForm>
    <div v-if="selected" class="mt-16px">
      <div class="mb-8px font-medium">{{ $t('page.mes.bomItem.title') }} {{ selected.versionNo }}</div>
      <DataTable :columns="itemColumns" :data="items" :bordered="false" size="small" />
      <NForm v-if="draftSelected" :model="itemForm" label-placement="left" label-width="96" class="mt-12px">
        <NFormItem :label="$t('page.mes.bomItem.lineNo')">
          <NInputNumber v-model:value="itemForm.lineNo" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.bomItem.material')">
          <NSelect v-model:value="itemForm.materialId" :options="materialOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.bomItem.qtyPer')">
          <NInputNumber v-model:value="itemForm.qtyPer" :min="0.000001" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.bomItem.uom')">
          <NSelect v-model:value="itemForm.uomId" :options="uomOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.bomItem.step')">
          <NSelect v-model:value="itemForm.stepId" :options="stationOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.bomItem.routeSeq')">
          <NInputNumber v-model:value="itemForm.routeSeqNo" :min="0" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.bomItem.consumeMode')">
          <NSelect v-model:value="itemForm.consumeMode" :options="modeOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.bomItem.substitutePolicy')">
          <NSelect v-model:value="itemForm.substitutePolicy" :options="policyOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.bomItem.substituteGroup')">
          <NInput v-model:value="itemForm.substituteGroup" />
        </NFormItem>
        <div class="flex justify-end gap-8px">
          <NButton @click="resetItemForm">{{ $t('common.reset') }}</NButton>
          <NButton v-if="!editingItem && hasAuth('mes:bom:add')" type="primary" @click="saveItem">{{ $t('common.add') }}</NButton>
          <NButton v-else-if="editingItem && hasAuth('mes:bom:edit')" type="primary" @click="saveItem">{{ $t('common.save') }}</NButton>
        </div>
      </NForm>
    </div>
  </NModal>
</template>
