<script setup lang="ts">
import { computed, h, reactive, ref, watch } from 'vue';
import { NButton, type DataTableColumns, type SelectOption } from 'naive-ui';
import { getEquipmentGroupList, type EquipmentGroup } from '@/service/api/mes/equipment-group';
import {
  createStepEquipmentGroup,
  deleteStepEquipmentGroup,
  getStepEquipmentGroupList,
  updateStepEquipmentGroup,
  type StepEquipmentGroup
} from '@/service/api/mes/step-equipment-group';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

const props = defineProps<{
  show: boolean;
  stepId: string;
  stepCode: string;
}>();

const emit = defineEmits<{ 'update:show': [value: boolean] }>();

const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<StepEquipmentGroup[]>([]);
const groups = ref<EquipmentGroup[]>([]);
const editing = ref<StepEquipmentGroup | null>(null);
const form = reactive({ groupId: null as string | null, priority: 0 as number | null });
const groupOptions = computed<SelectOption[]>(() => groups.value.map(item => ({ label: item.groupCode, value: item.id })));

const visible = computed({
  get: () => props.show,
  set: value => emit('update:show', value)
});

function groupLabel(id?: string | null) {
  return groups.value.find(item => item.id === id)?.groupCode ?? '';
}

function resetForm() {
  editing.value = null;
  form.groupId = null;
  form.priority = 0;
}

async function load() {
  loading.value = true;
  try {
    const [bindings, groupPage] = await Promise.all([
      getStepEquipmentGroupList(props.stepId),
      getEquipmentGroupList({ page: 1, pageSize: 200 })
    ]);
    rows.value = bindings.data ?? [];
    groups.value = groupPage.data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

watch(
  () => [props.show, props.stepId] as const,
  ([show]) => {
    if (show) {
      resetForm();
      load();
    }
  }
);

function editRow(row: StepEquipmentGroup) {
  editing.value = row;
  form.groupId = row.groupId;
  form.priority = row.priority;
}

async function save() {
  if (!form.groupId) {
    window.$message?.warning($t('page.mes.stepEquipmentGroup.required'));
    return;
  }
  const payload = { stepId: props.stepId, groupId: form.groupId, priority: form.priority ?? 0 };
  if (editing.value) {
    await updateStepEquipmentGroup(editing.value.id, payload);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createStepEquipmentGroup(payload);
    window.$message?.success($t('common.addSuccess'));
  }
  resetForm();
  await load();
}

function remove(row: StepEquipmentGroup) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.stepEquipmentGroup.deleteConfirm', { name: groupLabel(row.groupId) }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteStepEquipmentGroup(row.id);
      window.$message?.success($t('common.deleteSuccess'));
      await load();
    }
  });
}

const columns = computed<DataTableColumns<StepEquipmentGroup>>(() => [
  { title: $t('page.mes.stepEquipmentGroup.group'), key: 'groupId', render: row => groupLabel(row.groupId) },
  { title: $t('page.mes.stepEquipmentGroup.priority'), key: 'priority', width: 100 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 160,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        hasAuth('mes:step:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => editRow(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:step:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => remove(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);
</script>

<template>
  <NModal v-model:show="visible" preset="card" class="w-720px" :title="`${$t('page.mes.stepEquipmentGroup.title')} ${stepCode}`">
    <DataTable :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    <NForm :model="form" label-placement="left" label-width="96" class="mt-16px">
      <NFormItem :label="$t('page.mes.stepEquipmentGroup.group')">
        <NSelect v-model:value="form.groupId" :options="groupOptions" filterable />
      </NFormItem>
      <NFormItem :label="$t('page.mes.stepEquipmentGroup.priority')">
        <NInputNumber v-model:value="form.priority" class="w-full" />
      </NFormItem>
      <div class="flex justify-end gap-8px">
        <NButton @click="resetForm">{{ $t('common.reset') }}</NButton>
        <NButton v-if="!editing && hasAuth('mes:step:add')" type="primary" @click="save">{{ $t('common.add') }}</NButton>
        <NButton v-else-if="editing && hasAuth('mes:step:edit')" type="primary" @click="save">{{ $t('common.save') }}</NButton>
      </div>
    </NForm>
  </NModal>
</template>
