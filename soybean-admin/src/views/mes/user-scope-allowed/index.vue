<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { getAreaList, type Area } from '@/service/api/mes/area';
import { getEquipmentGroupList, type EquipmentGroup } from '@/service/api/mes/equipment-group';
import { getStepList, type Step } from '@/service/api/mes/step';
import { allowedUserScope } from '@/service/api/mes/user-scope';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { $t } from '@/locales';

defineOptions({ name: 'MesUserScopeAllowed' });

const TYPES = ['AREA', 'STEP', 'EQUIPMENT_GROUP'];
const areas = ref<Area[]>([]);
const steps = ref<Step[]>([]);
const groups = ref<EquipmentGroup[]>([]);
const userName = ref('');
const scopeType = ref('AREA');
const scopeId = ref<string | null>(null);
const answer = ref('');

const typeOptions = TYPES.map(item => ({ label: item, value: item }));
const objectOptions = computed(() => {
  if (scopeType.value === 'STEP') {
    return steps.value.map(item => ({ label: `${item.stepCode} ${item.stepName}`, value: String(item.id) }));
  }
  if (scopeType.value === 'EQUIPMENT_GROUP') {
    return groups.value.map(item => ({ label: `${item.groupCode} ${item.groupName}`, value: String(item.id) }));
  }
  return areas.value.map(item => ({ label: `${item.areaCode} ${item.areaName}`, value: String(item.id) }));
});

function onTypeChange() {
  scopeId.value = null;
  answer.value = '';
}

async function check() {
  if (!userName.value.trim() || !scopeId.value) {
    window.$message?.warning($t('page.mes.userScopeAllowed.required'));
    return;
  }
  const res = await allowedUserScope({
    userName: userName.value.trim(),
    scopeType: scopeType.value,
    scopeId: scopeId.value
  });
  answer.value = res.data ? $t('page.mes.userScopeAllowed.yes') : $t('page.mes.userScopeAllowed.no');
}

onMounted(async () => {
  const [areaPage, stepPage, groupPage] = await Promise.all([
    getAreaList({ page: 1, pageSize: 200 }),
    getStepList({ page: 1, pageSize: 200 }),
    getEquipmentGroupList({ page: 1, pageSize: 200 })
  ]);
  areas.value = areaPage.data?.records ?? [];
  steps.value = stepPage.data?.records ?? [];
  groups.value = groupPage.data?.records ?? [];
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>{{ $t('page.mes.userScopeAllowed.title') }}</template>
      <div class="mb-12px">{{ $t('page.mes.userScopeAllowed.hint') }}</div>
      <div class="flex flex-wrap items-center gap-8px">
        <NInput v-model:value="userName" class="w-180px" :placeholder="$t('page.mes.userScope.userName')" />
        <NSelect v-model:value="scopeType" class="w-200px" :options="typeOptions" @update:value="onTypeChange" />
        <NSelect v-model:value="scopeId" class="w-260px" :options="objectOptions" filterable />
        <PermissionGuard code="mes:user-scope:query">
          <NButton type="primary" @click="check">{{ $t('page.mes.userScopeAllowed.check') }}</NButton>
        </PermissionGuard>
        <span>{{ answer }}</span>
      </div>
    </NCard>
  </div>
</template>
