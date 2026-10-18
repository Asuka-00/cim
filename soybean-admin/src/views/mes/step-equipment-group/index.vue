<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getEquipmentGroupList, type EquipmentGroup } from '@/service/api/mes/equipment-group';
import { getStepList, type Step } from '@/service/api/mes/step';
import {
  createStepEquipmentGroup,
  deleteStepEquipmentGroup,
  getStepEquipmentGroupList,
  updateStepEquipmentGroup,
  type StepEquipmentGroup
} from '@/service/api/mes/step-equipment-group';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesStepEquipmentGroup' });

const { hasAuth } = useAuth();
const steps = ref<Step[]>([]);
const groups = ref<EquipmentGroup[]>([]);
const stepId = ref<string | null>(null);
const loading = ref(false);
const rows = ref<StepEquipmentGroup[]>([]);
const showModal = ref(false);
const editing = ref<StepEquipmentGroup | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({ groupId: null as string | null, priority: 0 as number | null });

const rules = computed<FormRules>(() => ({
  groupId: [{ required: true, message: $t('page.mes.stepEquipmentGroup.required'), trigger: 'change' }]
}));
const stepOptions = computed(() => steps.value.map(item => ({ label: `${item.stepCode} ${item.stepName}`, value: String(item.id) })));
const groupOptions = computed(() => groups.value.map(item => ({ label: `${item.groupCode} ${item.groupName}`, value: String(item.id) })));

function groupLabel(id?: string | null) {
  const group = groups.value.find(item => String(item.id) === String(id));
  return group ? `${group.groupCode} ${group.groupName}` : String(id ?? '');
}

function openCreate() {
  editing.value = null;
  Object.assign(form, { groupId: null, priority: 0 });
  showModal.value = true;
}

function openEdit(row: StepEquipmentGroup) {
  editing.value = row;
  Object.assign(form, { groupId: String(row.groupId), priority: row.priority ?? 0 });
  showModal.value = true;
}

const columns = computed<DataTableColumns<StepEquipmentGroup>>(() => [
  { title: $t('page.mes.stepEquipmentGroup.group'), key: 'groupId', render: row => groupLabel(row.groupId) },
  { title: $t('page.mes.stepEquipmentGroup.priority'), key: 'priority', width: 110 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 140,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        hasAuth('mes:step-equipment-group:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:step-equipment-group:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);

async function fetchData() {
  if (!stepId.value) {
    rows.value = [];
    return;
  }
  loading.value = true;
  try {
    const res = await getStepEquipmentGroupList(stepId.value);
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function submit() {
  await formRef.value?.validate();
  if (!stepId.value || !form.groupId) return;
  const data = { stepId: stepId.value, groupId: form.groupId, priority: form.priority ?? 0 };
  if (editing.value) await updateStepEquipmentGroup(String(editing.value.id), data);
  else await createStepEquipmentGroup(data);
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

function handleDelete(row: StepEquipmentGroup) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.stepEquipmentGroup.deleteConfirm', { name: groupLabel(row.groupId) }),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteStepEquipmentGroup(String(row.id));
      window.$message?.success($t('common.saveSuccess'));
      await fetchData();
    }
  });
}

onMounted(async () => {
  const [stepPage, groupPage] = await Promise.all([
    getStepList({ page: 1, pageSize: 200 }),
    getEquipmentGroupList({ page: 1, pageSize: 200 })
  ]);
  steps.value = stepPage.data?.records ?? [];
  groups.value = groupPage.data?.records ?? [];
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.stepEquipmentGroup.title') }}</span>
          <div class="flex items-center gap-8px">
            <NSelect v-model:value="stepId" class="w-260px" :options="stepOptions" filterable @update:value="fetchData" />
            <PermissionGuard code="mes:step-equipment-group:add">
              <NButton type="primary" :disabled="!stepId" @click="openCreate">{{ $t('common.add') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.stepEquipmentGroup.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog
      v-model:show="showModal"
      :title="editing ? $t('page.mes.stepEquipmentGroup.editTitle') : $t('page.mes.stepEquipmentGroup.addTitle')"
      class="w-560px"
      :on-confirm="submit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="110">
        <NFormItem :label="$t('page.mes.stepEquipmentGroup.group')" path="groupId">
          <NSelect v-model:value="form.groupId" :options="groupOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.stepEquipmentGroup.priority')" path="priority">
          <NInputNumber v-model:value="form.priority" class="w-full" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
