<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules, type SelectOption } from 'naive-ui';
import { getAreaList, type Area } from '@/service/api/mes/area';
import { getStepGroupList, type StepGroup } from '@/service/api/mes/step-group';
import { createStep, deleteStep, getStepList, updateStep, type Step } from '@/service/api/mes/step';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';
import EquipmentGroupDrawer from './equipment-group-drawer.vue';
import RecipeDrawer from './recipe-drawer.vue';

defineOptions({ name: 'MesStep' });

const NO_RESOURCE = new Set(['INSPECT', 'HOLD', 'SHIP', 'STORE']);
const { hasAuth } = useAuth();
const areas = ref<Area[]>([]);
const bindTarget = ref<Step | null>(null);
const bindShow = ref(false);
const recipeTarget = ref<Step | null>(null);
const recipeShow = ref(false);
const groups = ref<StepGroup[]>([]);

const typeOptions = ['PROCESS', 'MEASURE', 'INSPECT', 'HOLD', 'SHIP', 'CONVERT', 'PACK', 'STORE'].map(value => ({
  label: value,
  value
}));
const areaOptions = computed<SelectOption[]>(() => areas.value.map(item => ({ label: item.areaCode, value: item.id })));
const groupOptions = computed<SelectOption[]>(() => groups.value.map(item => ({ label: item.groupCode, value: item.id })));

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Step, { stepCode: string }>({
    initialQuery: { stepCode: '' },
    fetcher: getStepList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'stepCode', label: $t('page.mes.step.stepCode'), type: 'input' }
]);

const showModal = ref(false);
const editing = ref<Step | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  stepCode: '',
  stepName: '',
  stepType: 'PROCESS',
  areaId: null as string | null,
  stepGroupId: null as string | null,
  needEquipment: 1,
  needRecipe: 1,
  description: '',
  enabled: 1
});

const rules = computed<FormRules>(() => ({
  stepCode: [{ required: true, message: $t('page.mes.step.required'), trigger: 'blur' }],
  stepName: [{ required: true, message: $t('page.mes.step.required'), trigger: 'blur' }]
}));

function applyTypeDefault(type: string) {
  const flag = NO_RESOURCE.has(type) ? 0 : 1;
  form.needEquipment = flag;
  form.needRecipe = flag;
}

function openCreate() {
  editing.value = null;
  Object.assign(form, {
    stepCode: '',
    stepName: '',
    stepType: 'PROCESS',
    areaId: null,
    stepGroupId: null,
    needEquipment: 1,
    needRecipe: 1,
    description: '',
    enabled: 1
  });
  showModal.value = true;
}

function openEdit(row: Step) {
  editing.value = row;
  Object.assign(form, { ...row, areaId: row.areaId ?? null, stepGroupId: row.stepGroupId ?? null });
  showModal.value = true;
}

function areaLabel(id?: string | null) {
  return areas.value.find(item => item.id === id)?.areaCode ?? '';
}

const columns = computed<DataTableColumns<Step>>(() => [
  { title: $t('page.mes.step.stepCode'), key: 'stepCode' },
  { title: $t('page.mes.step.stepName'), key: 'stepName' },
  { title: $t('page.mes.step.stepType'), key: 'stepType', width: 110 },
  { title: $t('page.mes.step.area'), key: 'areaId', width: 120, render: row => areaLabel(row.areaId) },
  {
    title: $t('page.mes.step.needEquipment'),
    key: 'needEquipment',
    width: 90,
    render: row => (row.needEquipment === 1 ? $t('common.yesOrNo.yes') : $t('common.yesOrNo.no'))
  },
  {
    title: $t('common.status'),
    key: 'enabled',
    width: 90,
    render: row => (row.enabled === 1 ? $t('common.enabled') : $t('common.disabled'))
  },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 240,
    render: row =>
      h('div', { class: 'flex gap-8px flex-wrap' }, [
        h(
          NButton,
          {
            size: 'small',
            quaternary: true,
            onClick: () => {
              bindTarget.value = row;
              bindShow.value = true;
            }
          },
          () => $t('page.mes.stepEquipmentGroup.title')
        ),
        h(
          NButton,
          {
            size: 'small',
            quaternary: true,
            onClick: () => {
              recipeTarget.value = row;
              recipeShow.value = true;
            }
          },
          () => $t('page.mes.stepRecipe.title')
        ),
        hasAuth('mes:step:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:step:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  const payload = { ...form, areaId: form.areaId || null, stepGroupId: form.stepGroupId || null };
  if (editing.value) {
    await updateStep(String(editing.value.id), payload);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createStep(payload);
    window.$message?.success($t('common.addSuccess'));
  }
  await fetchData();
}

function handleDelete(row: Step) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.step.deleteConfirm', { name: row.stepCode }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteStep(String(row.id));
      window.$message?.success($t('common.deleteSuccess'));
      await fetchData();
    }
  });
}

onMounted(async () => {
  const [areaRes, groupRes] = await Promise.all([
    getAreaList({ page: 1, pageSize: 200 }),
    getStepGroupList({ page: 1, pageSize: 200 })
  ]);
  areas.value = areaRes.data?.records ?? [];
  groups.value = groupRes.data?.records ?? [];
  await fetchData();
});
</script>

<template>
  <div class="rtd-page">
    <FilterBar
      :fields="filterFields"
      :model-value="searchParams as Record<string, unknown>"
      @update:model-value="v => Object.assign(searchParams, v)"
      @search="handleSearch"
      @reset="handleReset"
    />
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.step.title') }}</span>
          <PermissionGuard code="mes:step:add">
            <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
          </PermissionGuard>
        </div>
      </template>
      <DataTable flex-height :columns="columns" :data="list" :loading="loading" :bordered="false" size="small" />
      <div class="mt-12px flex justify-end">
        <NPagination
          :page="currentPage"
          :page-size="pageSize"
          :item-count="total"
          show-size-picker
          :page-sizes="[10, 20, 50]"
          @update:page="onPageUpdate"
          @update:page-size="onSizeUpdate"
        />
      </div>
    </NCard>
    <CrudDialog
      v-model:show="showModal"
      :title="editing ? $t('page.mes.step.editTitle') : $t('page.mes.step.addTitle')"
      class="w-560px"
      :on-confirm="handleSubmit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="108">
        <NFormItem :label="$t('page.mes.step.stepCode')" path="stepCode">
          <NInput v-model:value="form.stepCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.step.stepName')" path="stepName">
          <NInput v-model:value="form.stepName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.step.stepType')" path="stepType">
          <NSelect v-model:value="form.stepType" :options="typeOptions" @update:value="applyTypeDefault" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.step.area')" path="areaId">
          <NSelect v-model:value="form.areaId" :options="areaOptions" filterable clearable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.step.stepGroup')" path="stepGroupId">
          <NSelect v-model:value="form.stepGroupId" :options="groupOptions" filterable clearable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.step.needEquipment')" path="needEquipment">
          <NRadioGroup v-model:value="form.needEquipment">
            <NRadio :value="1">{{ $t('common.yesOrNo.yes') }}</NRadio>
            <NRadio :value="0">{{ $t('common.yesOrNo.no') }}</NRadio>
          </NRadioGroup>
        </NFormItem>
        <NFormItem :label="$t('page.mes.step.needRecipe')" path="needRecipe">
          <NRadioGroup v-model:value="form.needRecipe">
            <NRadio :value="1">{{ $t('common.yesOrNo.yes') }}</NRadio>
            <NRadio :value="0">{{ $t('common.yesOrNo.no') }}</NRadio>
          </NRadioGroup>
        </NFormItem>
        <NFormItem :label="$t('page.mes.step.description')" path="description">
          <NInput v-model:value="form.description" type="textarea" :rows="2" />
        </NFormItem>
        <NFormItem :label="$t('common.status')" path="enabled">
          <NRadioGroup v-model:value="form.enabled">
            <NRadio :value="1">{{ $t('common.enabled') }}</NRadio>
            <NRadio :value="0">{{ $t('common.disabled') }}</NRadio>
          </NRadioGroup>
        </NFormItem>
      </NForm>
    </CrudDialog>
    <RecipeDrawer
      v-if="recipeTarget"
      v-model:show="recipeShow"
      :step-id="String(recipeTarget.id)"
      :step-code="recipeTarget.stepCode"
    />
    <EquipmentGroupDrawer
      v-if="bindTarget"
      v-model:show="bindShow"
      :step-id="String(bindTarget.id)"
      :step-code="bindTarget.stepCode"
    />
  </div>
</template>
