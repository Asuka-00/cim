<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { createUserQual, expireUserQuals, getUserQuals, type UserQual } from '@/service/api/mes/user-qual';
import { getEquipmentList, type Equipment } from '@/service/api/mes/equipment';
import { getEquipmentGroupList, type EquipmentGroup } from '@/service/api/mes/equipment-group';
import { getStepList, type Step } from '@/service/api/mes/step';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesUserQual' });

const TYPES = ['EQUIPMENT', 'EQUIPMENT_GROUP', 'STEP'];
const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<UserQual[]>([]);
const equipment = ref<Equipment[]>([]);
const groups = ref<EquipmentGroup[]>([]);
const steps = ref<Step[]>([]);
const showCreate = ref(false);
const formRef = ref<FormInst | null>(null);
const form = reactive({ userName: '', targetType: 'EQUIPMENT', targetId: null as string | null, validTo: null as string | null });

const rules = computed<FormRules>(() => ({
  userName: [{ required: true, message: $t('page.mes.userQual.required'), trigger: 'blur' }],
  targetType: [{ required: true, message: $t('page.mes.userQual.required'), trigger: 'change' }],
  targetId: [{ required: true, message: $t('page.mes.userQual.required'), trigger: 'change' }]
}));

const typeOptions = TYPES.map(item => ({ label: item, value: item }));
const targetOptions = computed(() => {
  if (form.targetType === 'EQUIPMENT_GROUP') {
    return groups.value.map(item => ({ label: `${item.groupCode} ${item.groupName}`, value: String(item.id) }));
  }
  if (form.targetType === 'STEP') {
    return steps.value.map(item => ({ label: `${item.stepCode} ${item.stepName}`, value: String(item.id) }));
  }
  return equipment.value.map(item => ({ label: `${item.equipmentCode} ${item.equipmentName}`, value: String(item.id) }));
});
const targetName = computed(() => {
  const names = new Map<string, string>();
  equipment.value.forEach(item => names.set(`EQUIPMENT:${item.id}`, `${item.equipmentCode} ${item.equipmentName}`));
  groups.value.forEach(item => names.set(`EQUIPMENT_GROUP:${item.id}`, `${item.groupCode} ${item.groupName}`));
  steps.value.forEach(item => names.set(`STEP:${item.id}`, `${item.stepCode} ${item.stepName}`));
  return (row: UserQual) => names.get(`${row.targetType}:${row.targetId}`) ?? row.targetId;
});

function openCreate() {
  Object.assign(form, { userName: '', targetType: 'EQUIPMENT', targetId: null, validTo: null });
  showCreate.value = true;
}

function onTypeChange() {
  form.targetId = null;
}

const columns = computed<DataTableColumns<UserQual>>(() => [
  { title: $t('page.mes.userQual.userName'), key: 'userName' },
  { title: $t('page.mes.userQual.targetType'), key: 'targetType', width: 160 },
  { title: $t('page.mes.userQual.target'), key: 'targetId', render: row => targetName.value(row) },
  { title: $t('page.mes.userQual.status'), key: 'status', width: 110 },
  { title: $t('page.mes.userQual.validTo'), key: 'validTo', width: 180 }
]);

async function fetchData() {
  loading.value = true;
  try {
    const [qualRes, equipmentPage, groupPage, stepPage] = await Promise.all([
      getUserQuals(),
      getEquipmentList({ page: 1, pageSize: 200 }),
      getEquipmentGroupList({ page: 1, pageSize: 200 }),
      getStepList({ page: 1, pageSize: 200 })
    ]);
    rows.value = qualRes.data ?? [];
    equipment.value = equipmentPage.data?.records ?? [];
    groups.value = groupPage.data?.records ?? [];
    steps.value = stepPage.data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

async function submitCreate() {
  await formRef.value?.validate();
  if (!form.targetId) return;
  await createUserQual({
    userName: form.userName,
    targetType: form.targetType,
    targetId: form.targetId,
    validTo: form.validTo
  });
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
}

async function handleExpire() {
  const res = await expireUserQuals();
  window.$message?.success($t('page.mes.userQual.expired', { count: res.data ?? 0 }));
  await fetchData();
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.userQual.title') }}</span>
          <div class="flex gap-8px">
            <PermissionGuard code="mes:qual:expire">
              <NButton @click="handleExpire">{{ $t('page.mes.userQual.expire') }}</NButton>
            </PermissionGuard>
            <PermissionGuard code="mes:qual:add">
              <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showCreate" :title="$t('page.mes.userQual.addTitle')" class="w-640px" :on-confirm="submitCreate">
      <div class="mb-12px">{{ $t('page.mes.userQual.hint') }}</div>
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.userQual.userName')" path="userName">
          <NInput v-model:value="form.userName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.userQual.targetType')" path="targetType">
          <NSelect v-model:value="form.targetType" :options="typeOptions" @update:value="onTypeChange" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.userQual.target')" path="targetId">
          <NSelect v-model:value="form.targetId" :options="targetOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.userQual.validTo')" path="validTo">
          <NDatePicker v-model:formatted-value="form.validTo" type="datetime" value-format="yyyy-MM-dd'T'HH:mm:ss" clearable class="w-full" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
