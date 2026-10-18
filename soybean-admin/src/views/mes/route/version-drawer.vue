<script setup lang="ts">
import { computed, h, reactive, ref, watch } from 'vue';
import { NButton, type DataTableColumns, type SelectOption } from 'naive-ui';
import { getCountUnitList, type CountUnit } from '@/service/api/mes/count-unit';
import { getProductList, type Product } from '@/service/api/mes/product';
import { getRecipeList, type Recipe } from '@/service/api/mes/recipe';
import {
  createRouteStepRecipe,
  deleteRouteStepRecipe,
  getOwnedRouteStepRecipes,
  updateRouteStepRecipe,
  type RouteStepRecipe
} from '@/service/api/mes/route-step-recipe';
import { getEquipmentGroupList, type EquipmentGroup } from '@/service/api/mes/equipment-group';
import {
  createRouteStep,
  deleteRouteStep,
  getRouteStepList,
  updateRouteStep,
  type RouteStep
} from '@/service/api/mes/route-step';
import {
  createRouteTimeLink,
  deleteRouteTimeLink,
  getRouteTimeLinkList,
  updateRouteTimeLink,
  type RouteTimeLink
} from '@/service/api/mes/route-time-link';
import {
  approveRouteVersion,
  copyRouteVersion,
  createRouteVersion,
  getRouteVersionList,
  rejectRouteVersion,
  submitRouteVersion,
  updateRouteVersion,
  type RouteVersion
} from '@/service/api/mes/route-version';
import { getStepList, type Step } from '@/service/api/mes/step';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

const props = defineProps<{
  show: boolean;
  routeId: string;
  routeCode: string;
}>();

const emit = defineEmits<{ 'update:show': [value: boolean] }>();

const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<RouteVersion[]>([]);
const editing = ref<RouteVersion | null>(null);
const form = reactive({ remark: '', actor: '' });
const selected = ref<RouteVersion | null>(null);
const steps = ref<RouteStep[]>([]);
const links = ref<RouteTimeLink[]>([]);
const editingLink = ref<RouteTimeLink | null>(null);
const linkForm = reactive({
  fromSeqNo: null as number | null,
  toSeqNo: null as number | null,
  maxMinutes: null as number | null,
  warnMinutes: null as number | null,
  action: 'HOLD',
  reasonCode: ''
});
const actionOptions = [
  { label: 'HOLD', value: 'HOLD' },
  { label: 'WARN', value: 'WARN' }
];
const stations = ref<Step[]>([]);
const groups = ref<EquipmentGroup[]>([]);
const units = ref<CountUnit[]>([]);
const recipes = ref<Recipe[]>([]);
const products = ref<Product[]>([]);
const selectedStep = ref<RouteStep | null>(null);
const overrides = ref<RouteStepRecipe[]>([]);
const editingOverride = ref<RouteStepRecipe | null>(null);
const overrideForm = reactive({ recipeId: null as string | null, productId: null as string | null, isDefault: 0 });
const recipeOptions = computed<SelectOption[]>(() => recipes.value.map(item => ({ label: item.recipeCode, value: item.id })));
const productOptions = computed<SelectOption[]>(() => products.value.map(item => ({ label: item.productCode, value: item.id })));
const editingStep = ref<RouteStep | null>(null);
const stepForm = reactive({
  seqNo: null as number | null,
  stepId: null as string | null,
  stepAlias: '',
  isRequired: 1,
  equipmentGroupId: null as string | null,
  countUnitId: null as string | null,
  isUnitConvert: 0,
  convertToUnitId: null as string | null,
  queueTimeMaxMin: null as number | null,
  queueTimeWarnMin: null as number | null,
  queueTimeAction: null as string | null
});
const stationOptions = computed<SelectOption[]>(() => stations.value.map(item => ({ label: item.stepCode, value: item.id })));
const groupOptions = computed<SelectOption[]>(() => groups.value.map(item => ({ label: item.groupCode, value: item.id })));
const unitOptions = computed<SelectOption[]>(() => units.value.map(item => ({ label: item.unitCode, value: item.id })));
const draftSelected = computed(() => selected.value?.status === 'DRAFT');

const visible = computed({
  get: () => props.show,
  set: value => emit('update:show', value)
});

function resetForm() {
  editing.value = null;
  form.remark = '';
}

function resetStepForm() {
  editingStep.value = null;
  Object.assign(stepForm, {
    seqNo: null,
    stepId: null,
    stepAlias: '',
    isRequired: 1,
    equipmentGroupId: null,
    countUnitId: null,
    isUnitConvert: 0,
    convertToUnitId: null,
    queueTimeMaxMin: null,
    queueTimeWarnMin: null,
    queueTimeAction: null
  });
}

function resetLinkForm() {
  editingLink.value = null;
  Object.assign(linkForm, { fromSeqNo: null, toSeqNo: null, maxMinutes: null, warnMinutes: null, action: 'HOLD', reasonCode: '' });
}

async function loadSteps(version: RouteVersion) {
  selected.value = version;
  const [stepResult, linkResult] = await Promise.all([getRouteStepList(version.id), getRouteTimeLinkList(version.id)]);
  steps.value = stepResult.data ?? [];
  links.value = linkResult.data ?? [];
  selectedStep.value = null;
  overrides.value = [];
}

async function loadMasters() {
  const [stepPage, groupPage, unitPage, recipePage, productPage] = await Promise.all([
    getStepList({ page: 1, pageSize: 200 }),
    getEquipmentGroupList({ page: 1, pageSize: 200 }),
    getCountUnitList({ page: 1, pageSize: 200 }),
    getRecipeList({ page: 1, pageSize: 200 }),
    getProductList({ page: 1, pageSize: 200 })
  ]);
  stations.value = stepPage.data?.records ?? [];
  groups.value = groupPage.data?.records ?? [];
  units.value = unitPage.data?.records ?? [];
  recipes.value = recipePage.data?.records ?? [];
  products.value = productPage.data?.records ?? [];
}

async function load() {
  if (!props.routeId) return;
  loading.value = true;
  try {
    const { data } = await getRouteVersionList(props.routeId, { page: 1, pageSize: 50 });
    rows.value = data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

watch(
  () => [props.show, props.routeId] as const,
  ([show]) => {
    if (show) {
      resetForm();
      resetStepForm();
      resetLinkForm();
      selected.value = null;
      steps.value = [];
      links.value = [];
      load();
      loadMasters();
    }
  }
);

function editRow(row: RouteVersion) {
  editing.value = row;
  form.remark = row.remark ?? '';
}

function requireActor() {
  if (!form.actor.trim()) {
    window.$message?.warning($t('page.mes.routeVersion.actorRequired'));
    return false;
  }
  return true;
}

async function save() {
  if (editing.value) {
    await updateRouteVersion(props.routeId, editing.value.id, { remark: form.remark });
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createRouteVersion(props.routeId, { remark: form.remark });
    window.$message?.success($t('common.addSuccess'));
  }
  resetForm();
  await load();
}

async function copy(row: RouteVersion) {
  await copyRouteVersion(props.routeId, row.id);
  window.$message?.success($t('common.addSuccess'));
  await load();
}

async function submit(row: RouteVersion) {
  if (!requireActor()) return;
  await submitRouteVersion(props.routeId, row.id, { assignee: form.actor.trim() });
  window.$message?.success($t('page.mes.routeVersion.submitted'));
  await load();
}

async function approve(row: RouteVersion) {
  if (!requireActor()) return;
  await approveRouteVersion(props.routeId, row.id, { operator: form.actor.trim() });
  window.$message?.success($t('page.mes.routeVersion.approved'));
  await load();
}

async function reject(row: RouteVersion) {
  if (!requireActor()) return;
  await rejectRouteVersion(props.routeId, row.id, { operator: form.actor.trim() });
  window.$message?.success($t('page.mes.routeVersion.rejected'));
  await load();
}

const columns = computed<DataTableColumns<RouteVersion>>(() => [
  { title: $t('page.mes.routeVersion.versionNo'), key: 'versionNo', width: 80 },
  { title: $t('page.mes.routeVersion.status'), key: 'status', width: 110 },
  { title: $t('common.remark'), key: 'remark' },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 280,
    render: row =>
      h('div', { class: 'flex gap-8px flex-wrap' }, [
        h(NButton, { size: 'small', quaternary: true, onClick: () => loadSteps(row) }, () => $t('page.mes.routeStep.title')),
        hasAuth('mes:route:edit')
          ? h(NButton, { size: 'small', quaternary: true, onClick: () => copy(row) }, () => $t('page.mes.routeVersion.copy'))
          : null,
        row.status === 'DRAFT' && hasAuth('mes:route:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => editRow(row) }, () => $t('common.edit'))
          : null,
        row.status === 'DRAFT' && hasAuth('mes:route:edit')
          ? h(NButton, { size: 'small', quaternary: true, onClick: () => submit(row) }, () => $t('page.mes.routeVersion.submit'))
          : null,
        row.status === 'PENDING' && hasAuth('mes:route:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'success', onClick: () => approve(row) }, () =>
              $t('page.mes.routeVersion.approve')
            )
          : null,
        row.status === 'PENDING' && hasAuth('mes:route:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => reject(row) }, () =>
              $t('page.mes.routeVersion.reject')
            )
          : null
      ])
  }
]);

function stationLabel(id?: string | null) {
  return stations.value.find(item => item.id === id)?.stepCode ?? '';
}

function editStep(row: RouteStep) {
  editingStep.value = row;
  Object.assign(stepForm, {
    seqNo: row.seqNo,
    stepId: row.stepId,
    stepAlias: row.stepAlias ?? '',
    isRequired: row.isRequired,
    equipmentGroupId: row.equipmentGroupId ?? null,
    countUnitId: row.countUnitId ?? null,
    isUnitConvert: row.isUnitConvert,
    convertToUnitId: row.convertToUnitId ?? null,
    queueTimeMaxMin: row.queueTimeMaxMin ?? null,
    queueTimeWarnMin: row.queueTimeWarnMin ?? null,
    queueTimeAction: row.queueTimeAction ?? null
  });
}

async function saveStep() {
  if (!selected.value || stepForm.seqNo == null || !stepForm.stepId) {
    window.$message?.warning($t('page.mes.routeStep.required'));
    return;
  }
  const payload = {
    ...stepForm,
    equipmentGroupId: stepForm.equipmentGroupId || null,
    countUnitId: stepForm.countUnitId || null,
    convertToUnitId: stepForm.isUnitConvert === 1 ? stepForm.convertToUnitId : null,
    queueTimeAction: stepForm.queueTimeAction || null
  };
  if (editingStep.value) {
    await updateRouteStep(selected.value.id, editingStep.value.id, payload);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createRouteStep(selected.value.id, payload);
    window.$message?.success($t('common.addSuccess'));
  }
  resetStepForm();
  await loadSteps(selected.value);
}

function removeStep(row: RouteStep) {
  if (!selected.value) return;
  const version = selected.value;
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.routeStep.deleteConfirm', { name: String(row.seqNo) }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteRouteStep(version.id, row.id);
      window.$message?.success($t('common.deleteSuccess'));
      await loadSteps(version);
    }
  });
}

const stepColumns = computed<DataTableColumns<RouteStep>>(() => [
  { title: $t('page.mes.routeStep.seqNo'), key: 'seqNo', width: 80 },
  { title: $t('page.mes.routeStep.step'), key: 'stepId', render: row => stationLabel(row.stepId) },
  { title: $t('page.mes.routeStep.alias'), key: 'stepAlias' },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 220,
    render: row =>
      draftSelected.value
        ? h('div', { class: 'flex gap-8px flex-wrap' }, [
            h(NButton, { size: 'small', quaternary: true, onClick: () => loadOverrides(row) }, () => $t('page.mes.routeStepRecipe.title')),
            hasAuth('mes:route:edit')
              ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => editStep(row) }, () => $t('common.edit'))
              : null,
            hasAuth('mes:route:delete')
              ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => removeStep(row) }, () => $t('common.delete'))
              : null
          ])
        : $t('page.mes.routeStep.readonly')
  }
]);

function optionLabel(options: SelectOption[], id?: string | null) {
  return String(options.find(item => item.value === id)?.label ?? '');
}

async function loadOverrides(step: RouteStep) {
  selectedStep.value = step;
  resetOverrideForm();
  const { data } = await getOwnedRouteStepRecipes(step.id);
  overrides.value = data ?? [];
}

function resetOverrideForm() {
  editingOverride.value = null;
  overrideForm.recipeId = null;
  overrideForm.productId = null;
  overrideForm.isDefault = 0;
}

function editOverride(row: RouteStepRecipe) {
  editingOverride.value = row;
  overrideForm.recipeId = row.recipeId;
  overrideForm.productId = row.productId === '0' ? null : row.productId;
  overrideForm.isDefault = row.isDefault;
}

async function saveOverride() {
  if (!selectedStep.value || !overrideForm.recipeId) {
    window.$message?.warning($t('page.mes.routeStepRecipe.required'));
    return;
  }
  const payload = {
    routeStepId: selectedStep.value.id,
    recipeId: overrideForm.recipeId,
    productId: overrideForm.productId || null,
    isDefault: overrideForm.isDefault
  };
  if (editingOverride.value) {
    await updateRouteStepRecipe(editingOverride.value.id, payload);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createRouteStepRecipe(payload);
    window.$message?.success($t('common.addSuccess'));
  }
  await loadOverrides(selectedStep.value);
}

function removeOverride(row: RouteStepRecipe) {
  if (!selectedStep.value) return;
  const step = selectedStep.value;
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.routeStepRecipe.deleteConfirm'),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteRouteStepRecipe(row.id);
      window.$message?.success($t('common.deleteSuccess'));
      await loadOverrides(step);
    }
  });
}

const overrideColumns = computed<DataTableColumns<RouteStepRecipe>>(() => [
  { title: $t('page.mes.routeStepRecipe.recipe'), key: 'recipeId', render: row => optionLabel(recipeOptions.value, row.recipeId) },
  {
    title: $t('page.mes.routeStepRecipe.product'),
    key: 'productId',
    render: row => (row.productId === '0' ? $t('page.mes.routeStepRecipe.generic') : optionLabel(productOptions.value, row.productId))
  },
  {
    title: $t('page.mes.routeStepRecipe.defaultRecipe'),
    key: 'isDefault',
    width: 80,
    render: row => (row.isDefault === 1 ? $t('common.yesOrNo.yes') : $t('common.yesOrNo.no'))
  },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 140,
    render: row =>
      draftSelected.value
        ? h('div', { class: 'flex gap-8px' }, [
            hasAuth('mes:route:edit')
              ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => editOverride(row) }, () => $t('common.edit'))
              : null,
            hasAuth('mes:route:delete')
              ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => removeOverride(row) }, () => $t('common.delete'))
              : null
          ])
        : $t('page.mes.routeStep.readonly')
  }
]);

function editLink(row: RouteTimeLink) {
  editingLink.value = row;
  Object.assign(linkForm, {
    fromSeqNo: row.fromSeqNo,
    toSeqNo: row.toSeqNo,
    maxMinutes: row.maxMinutes,
    warnMinutes: row.warnMinutes ?? null,
    action: row.action,
    reasonCode: row.reasonCode ?? ''
  });
}

async function saveLink() {
  if (!selected.value || linkForm.fromSeqNo == null || linkForm.toSeqNo == null || linkForm.maxMinutes == null) {
    window.$message?.warning($t('page.mes.routeTimeLink.required'));
    return;
  }
  if (linkForm.action === 'HOLD' && !linkForm.reasonCode.trim()) {
    window.$message?.warning($t('page.mes.routeTimeLink.reasonRequired'));
    return;
  }
  const payload = { ...linkForm, reasonCode: linkForm.reasonCode.trim() };
  if (editingLink.value) {
    await updateRouteTimeLink(selected.value.id, editingLink.value.id, payload);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createRouteTimeLink(selected.value.id, payload);
    window.$message?.success($t('common.addSuccess'));
  }
  resetLinkForm();
  await loadSteps(selected.value);
}

function removeLink(row: RouteTimeLink) {
  if (!selected.value) return;
  const version = selected.value;
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.routeTimeLink.deleteConfirm', { from: String(row.fromSeqNo), to: String(row.toSeqNo) }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteRouteTimeLink(version.id, row.id);
      window.$message?.success($t('common.deleteSuccess'));
      await loadSteps(version);
    }
  });
}

const linkColumns = computed<DataTableColumns<RouteTimeLink>>(() => [
  { title: $t('page.mes.routeTimeLink.fromSeq'), key: 'fromSeqNo', width: 90 },
  { title: $t('page.mes.routeTimeLink.toSeq'), key: 'toSeqNo', width: 90 },
  { title: $t('page.mes.routeTimeLink.maxMinutes'), key: 'maxMinutes', width: 110 },
  { title: $t('page.mes.routeTimeLink.action'), key: 'action', width: 90 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 140,
    render: row =>
      draftSelected.value
        ? h('div', { class: 'flex gap-8px' }, [
            hasAuth('mes:route:edit')
              ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => editLink(row) }, () => $t('common.edit'))
              : null,
            hasAuth('mes:route:delete')
              ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => removeLink(row) }, () => $t('common.delete'))
              : null
          ])
        : $t('page.mes.routeStep.readonly')
  }
]);
</script>

<template>
  <NModal v-model:show="visible" preset="card" class="w-960px" :title="`${$t('page.mes.routeVersion.title')} ${routeCode}`">
    <DataTable :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    <NForm :model="form" label-placement="left" label-width="96" class="mt-16px">
      <NFormItem :label="$t('common.remark')">
        <NInput v-model:value="form.remark" />
      </NFormItem>
      <NFormItem :label="$t('page.mes.routeVersion.actor')">
        <NInput v-model:value="form.actor" :placeholder="$t('page.mes.routeVersion.actorHint')" />
      </NFormItem>
      <div class="flex justify-end gap-8px">
        <NButton @click="resetForm">{{ $t('common.reset') }}</NButton>
        <NButton v-if="!editing && hasAuth('mes:route:add')" type="primary" @click="save">{{ $t('common.add') }}</NButton>
        <NButton v-else-if="editing && hasAuth('mes:route:edit')" type="primary" @click="save">{{ $t('common.save') }}</NButton>
      </div>
    </NForm>
    <div v-if="selected" class="mt-16px">
      <div class="mb-8px font-medium">{{ $t('page.mes.routeStep.title') }} {{ selected.versionNo }}</div>
      <DataTable :columns="stepColumns" :data="steps" :bordered="false" size="small" />
      <NForm v-if="draftSelected" :model="stepForm" label-placement="left" label-width="96" class="mt-12px">
        <NFormItem :label="$t('page.mes.routeStep.seqNo')">
          <NInputNumber v-model:value="stepForm.seqNo" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeStep.step')">
          <NSelect v-model:value="stepForm.stepId" :options="stationOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeStep.alias')">
          <NInput v-model:value="stepForm.stepAlias" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeStep.equipmentGroup')">
          <NSelect v-model:value="stepForm.equipmentGroupId" :options="groupOptions" filterable clearable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeStep.countUnit')">
          <NSelect v-model:value="stepForm.countUnitId" :options="unitOptions" filterable clearable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeStep.queueMax')">
          <NInputNumber v-model:value="stepForm.queueTimeMaxMin" :min="1" class="w-full" clearable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeStep.queueWarn')">
          <NInputNumber v-model:value="stepForm.queueTimeWarnMin" :min="1" class="w-full" clearable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeStep.queueAction')">
          <NSelect v-model:value="stepForm.queueTimeAction" :options="actionOptions" clearable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeStep.unitConvert')">
          <NRadioGroup v-model:value="stepForm.isUnitConvert">
            <NRadio :value="1">{{ $t('common.yesOrNo.yes') }}</NRadio>
            <NRadio :value="0">{{ $t('common.yesOrNo.no') }}</NRadio>
          </NRadioGroup>
        </NFormItem>
        <NFormItem v-if="stepForm.isUnitConvert === 1" :label="$t('page.mes.routeStep.convertUnit')">
          <NSelect v-model:value="stepForm.convertToUnitId" :options="unitOptions" filterable />
        </NFormItem>
        <div class="flex justify-end gap-8px">
          <NButton @click="resetStepForm">{{ $t('common.reset') }}</NButton>
          <NButton v-if="!editingStep && hasAuth('mes:route:add')" type="primary" @click="saveStep">{{ $t('common.add') }}</NButton>
          <NButton v-else-if="editingStep && hasAuth('mes:route:edit')" type="primary" @click="saveStep">{{ $t('common.save') }}</NButton>
        </div>
      </NForm>
      <div v-if="selectedStep" class="mt-16px">
        <div class="mb-8px font-medium">{{ $t('page.mes.routeStepRecipe.title') }} {{ selectedStep.seqNo }}</div>
        <DataTable :columns="overrideColumns" :data="overrides" :bordered="false" size="small" />
        <NForm v-if="draftSelected" :model="overrideForm" label-placement="left" label-width="96" class="mt-12px">
          <NFormItem :label="$t('page.mes.routeStepRecipe.recipe')">
            <NSelect v-model:value="overrideForm.recipeId" :options="recipeOptions" filterable />
          </NFormItem>
          <NFormItem :label="$t('page.mes.routeStepRecipe.product')">
            <NSelect v-model:value="overrideForm.productId" :options="productOptions" filterable clearable />
          </NFormItem>
          <NFormItem :label="$t('page.mes.routeStepRecipe.defaultRecipe')">
            <NRadioGroup v-model:value="overrideForm.isDefault">
              <NRadio :value="1">{{ $t('common.yesOrNo.yes') }}</NRadio>
              <NRadio :value="0">{{ $t('common.yesOrNo.no') }}</NRadio>
            </NRadioGroup>
          </NFormItem>
          <div class="flex justify-end gap-8px">
            <NButton @click="resetOverrideForm">{{ $t('common.reset') }}</NButton>
            <NButton v-if="!editingOverride && hasAuth('mes:route:add')" type="primary" @click="saveOverride">{{ $t('common.add') }}</NButton>
            <NButton v-else-if="editingOverride && hasAuth('mes:route:edit')" type="primary" @click="saveOverride">
              {{ $t('common.save') }}
            </NButton>
          </div>
        </NForm>
      </div>
      <div class="mb-8px mt-16px font-medium">{{ $t('page.mes.routeTimeLink.title') }}</div>
      <DataTable :columns="linkColumns" :data="links" :bordered="false" size="small" />
      <NForm v-if="draftSelected" :model="linkForm" label-placement="left" label-width="96" class="mt-12px">
        <NFormItem :label="$t('page.mes.routeTimeLink.fromSeq')">
          <NInputNumber v-model:value="linkForm.fromSeqNo" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeTimeLink.toSeq')">
          <NInputNumber v-model:value="linkForm.toSeqNo" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeTimeLink.maxMinutes')">
          <NInputNumber v-model:value="linkForm.maxMinutes" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeTimeLink.warnMinutes')">
          <NInputNumber v-model:value="linkForm.warnMinutes" :min="1" class="w-full" clearable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeTimeLink.action')">
          <NSelect v-model:value="linkForm.action" :options="actionOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeTimeLink.reasonCode')">
          <NInput v-model:value="linkForm.reasonCode" />
        </NFormItem>
        <div class="flex justify-end gap-8px">
          <NButton @click="resetLinkForm">{{ $t('common.reset') }}</NButton>
          <NButton v-if="!editingLink && hasAuth('mes:route:add')" type="primary" @click="saveLink">{{ $t('common.add') }}</NButton>
          <NButton v-else-if="editingLink && hasAuth('mes:route:edit')" type="primary" @click="saveLink">{{ $t('common.save') }}</NButton>
        </div>
      </NForm>
    </div>
  </NModal>
</template>
