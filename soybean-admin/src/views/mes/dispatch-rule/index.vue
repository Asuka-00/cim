<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { createDispatchRule, getDispatchRules, type DispatchRule } from '@/service/api/mes/dispatch-rule';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { $t } from '@/locales';

defineOptions({ name: 'MesDispatchRule' });

const loading = ref(false);
const rows = ref<DispatchRule[]>([]);
const showCreate = ref(false);
const formRef = ref<FormInst | null>(null);
const form = reactive({ ruleCode: '', ruleName: '', sortKeys: 'PRIORITY:ASC,STEP_ENTER_AT:ASC', isDefault: false });

const rules = computed<FormRules>(() => ({
  ruleCode: [{ required: true, message: $t('page.mes.dispatch.required'), trigger: 'blur' }],
  ruleName: [{ required: true, message: $t('page.mes.dispatch.required'), trigger: 'blur' }],
  sortKeys: [{ required: true, message: $t('page.mes.dispatch.required'), trigger: 'blur' }]
}));

function openCreate() {
  Object.assign(form, { ruleCode: '', ruleName: '', sortKeys: 'PRIORITY:ASC,STEP_ENTER_AT:ASC', isDefault: false });
  showCreate.value = true;
}

const columns = computed<DataTableColumns<DispatchRule>>(() => [
  { title: $t('page.mes.dispatch.code'), key: 'ruleCode', width: 160 },
  { title: $t('page.mes.dispatch.name'), key: 'ruleName' },
  { title: $t('page.mes.dispatch.keys'), key: 'sortKeys' },
  {
    title: $t('page.mes.dispatch.default'),
    key: 'isDefault',
    width: 90,
    render: row => (row.isDefault === 1 ? $t('page.mes.dispatch.yes') : '')
  }
]);

async function fetchData() {
  loading.value = true;
  try {
    const res = await getDispatchRules();
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function submitCreate() {
  await formRef.value?.validate();
  await createDispatchRule({
    ruleCode: form.ruleCode,
    ruleName: form.ruleName,
    sortKeys: form.sortKeys,
    isDefault: form.isDefault ? 1 : 0
  });
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
          <span>{{ $t('page.mes.dispatch.title') }}</span>
          <PermissionGuard code="mes:dispatch:add">
            <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
          </PermissionGuard>
        </div>
      </template>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showCreate" :title="$t('page.mes.dispatch.addTitle')" class="w-640px" :on-confirm="submitCreate">
      <div class="mb-12px">{{ $t('page.mes.dispatch.hint') }}</div>
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.dispatch.code')" path="ruleCode">
          <NInput v-model:value="form.ruleCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.dispatch.name')" path="ruleName">
          <NInput v-model:value="form.ruleName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.dispatch.keys')" path="sortKeys">
          <NInput v-model:value="form.sortKeys" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.dispatch.default')" path="isDefault">
          <NSwitch v-model:value="form.isDefault" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
