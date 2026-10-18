<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { createUserScope, getUserScopes, type UserScope } from '@/service/api/mes/user-scope';
import { getAreaList, type Area } from '@/service/api/mes/area';
import { getEquipmentGroupList, type EquipmentGroup } from '@/service/api/mes/equipment-group';
import { getStepList, type Step } from '@/service/api/mes/step';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { $t } from '@/locales';

defineOptions({ name: 'MesUserScope' });

const TYPES = ['AREA', 'STEP', 'EQUIPMENT_GROUP'];
const loading = ref(false);
const rows = ref<UserScope[]>([]);
const areas = ref<Area[]>([]);
const groups = ref<EquipmentGroup[]>([]);
const steps = ref<Step[]>([]);
const showCreate = ref(false);
const formRef = ref<FormInst | null>(null);
const form = reactive({ userName: '', scopeType: 'AREA', scopeId: null as string | null });

const rules = computed<FormRules>(() => ({
  userName: [{ required: true, message: $t('page.mes.userScope.required'), trigger: 'blur' }],
  scopeType: [{ required: true, message: $t('page.mes.userScope.required'), trigger: 'change' }],
  scopeId: [{ required: true, message: $t('page.mes.userScope.required'), trigger: 'change' }]
}));

const typeOptions = TYPES.map(item => ({ label: item, value: item }));
const targetOptions = computed(() => {
  if (form.scopeType === 'STEP') {
    return steps.value.map(item => ({ label: `${item.stepCode} ${item.stepName}`, value: String(item.id) }));
  }
  if (form.scopeType === 'EQUIPMENT_GROUP') {
    return groups.value.map(item => ({ label: `${item.groupCode} ${item.groupName}`, value: String(item.id) }));
  }
  return areas.value.map(item => ({ label: `${item.areaCode} ${item.areaName}`, value: String(item.id) }));
});
const targetName = computed(() => {
  const names = new Map<string, string>();
  areas.value.forEach(item => names.set(`AREA:${item.id}`, `${item.areaCode} ${item.areaName}`));
  steps.value.forEach(item => names.set(`STEP:${item.id}`, `${item.stepCode} ${item.stepName}`));
  groups.value.forEach(item => names.set(`EQUIPMENT_GROUP:${item.id}`, `${item.groupCode} ${item.groupName}`));
  return (row: UserScope) => names.get(`${row.scopeType}:${row.scopeId}`) ?? row.scopeId;
});

function openCreate() {
  Object.assign(form, { userName: '', scopeType: 'AREA', scopeId: null });
  showCreate.value = true;
}

function onTypeChange() {
  form.scopeId = null;
}

const columns = computed<DataTableColumns<UserScope>>(() => [
  { title: $t('page.mes.userScope.userName'), key: 'userName' },
  { title: $t('page.mes.userScope.scopeType'), key: 'scopeType', width: 160 },
  { title: $t('page.mes.userScope.target'), key: 'scopeId', render: row => targetName.value(row) }
]);

async function fetchData() {
  loading.value = true;
  try {
    const [scopeRes, areaPage, stepPage, groupPage] = await Promise.all([
      getUserScopes(),
      getAreaList({ page: 1, pageSize: 200 }),
      getStepList({ page: 1, pageSize: 200 }),
      getEquipmentGroupList({ page: 1, pageSize: 200 })
    ]);
    rows.value = scopeRes.data ?? [];
    areas.value = areaPage.data?.records ?? [];
    steps.value = stepPage.data?.records ?? [];
    groups.value = groupPage.data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

async function submitCreate() {
  await formRef.value?.validate();
  if (!form.scopeId) return;
  await createUserScope({ userName: form.userName, scopeType: form.scopeType, scopeId: form.scopeId });
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.userScope.title') }}</span>
          <PermissionGuard code="mes:scope:add">
            <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
          </PermissionGuard>
        </div>
      </template>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showCreate" :title="$t('page.mes.userScope.addTitle')" class="w-640px" :on-confirm="submitCreate">
      <div class="mb-12px">{{ $t('page.mes.userScope.hint') }}</div>
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.userScope.userName')" path="userName">
          <NInput v-model:value="form.userName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.userScope.scopeType')" path="scopeType">
          <NSelect v-model:value="form.scopeType" :options="typeOptions" @update:value="onTypeChange" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.userScope.target')" path="scopeId">
          <NSelect v-model:value="form.scopeId" :options="targetOptions" filterable />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
