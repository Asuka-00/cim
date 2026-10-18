<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { createConfirmRule, getConfirmRules, signAction, type ConfirmRule } from '@/service/api/mes/confirm-rule';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesConfirmRule' });

const MODES = ['REAUTH', 'SECOND_SIGN'];
const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<ConfirmRule[]>([]);
const showCreate = ref(false);
const showSign = ref(false);
const createRef = ref<FormInst | null>(null);
const signRef = ref<FormInst | null>(null);
const current = ref<ConfirmRule | null>(null);
const createForm = reactive({ actionCode: '', confirmMode: 'REAUTH', secondRoleCode: '' });
const signForm = reactive({ bizType: '', bizId: '', signer: '' });

const createRules = computed<FormRules>(() => ({
  actionCode: [{ required: true, message: $t('page.mes.confirm.required'), trigger: 'blur' }],
  confirmMode: [{ required: true, message: $t('page.mes.confirm.required'), trigger: 'change' }],
  secondRoleCode:
    createForm.confirmMode === 'SECOND_SIGN'
      ? [{ required: true, message: $t('page.mes.confirm.required'), trigger: 'blur' }]
      : []
}));
const signRules = computed<FormRules>(() => ({
  bizType: [{ required: true, message: $t('page.mes.confirm.required'), trigger: 'blur' }],
  bizId: [{ required: true, message: $t('page.mes.confirm.required'), trigger: 'blur' }],
  signer:
    current.value?.confirmMode === 'SECOND_SIGN'
      ? [{ required: true, message: $t('page.mes.confirm.required'), trigger: 'blur' }]
      : []
}));
const modeOptions = MODES.map(item => ({ label: item, value: item }));

function openCreate() {
  Object.assign(createForm, { actionCode: '', confirmMode: 'REAUTH', secondRoleCode: '' });
  showCreate.value = true;
}

function openSign(row: ConfirmRule) {
  current.value = row;
  Object.assign(signForm, { bizType: '', bizId: '', signer: '' });
  showSign.value = true;
}

const columns = computed<DataTableColumns<ConfirmRule>>(() => [
  { title: $t('page.mes.confirm.action'), key: 'actionCode' },
  { title: $t('page.mes.confirm.mode'), key: 'confirmMode', width: 140 },
  { title: $t('page.mes.confirm.role'), key: 'secondRoleCode' },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 90,
    render: row =>
      row.enabled === 1 && hasAuth('mes:confirm:sign')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openSign(row) }, () => $t('page.mes.confirm.sign'))
        : null
  }
]);

async function fetchData() {
  loading.value = true;
  try {
    const res = await getConfirmRules();
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function submitCreate() {
  await createRef.value?.validate();
  await createConfirmRule({
    actionCode: createForm.actionCode,
    confirmMode: createForm.confirmMode,
    secondRoleCode: createForm.confirmMode === 'SECOND_SIGN' ? createForm.secondRoleCode : undefined
  });
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
}

async function submitSign() {
  await signRef.value?.validate();
  if (!current.value) return;
  await signAction({
    actionCode: current.value.actionCode,
    bizType: signForm.bizType,
    bizId: signForm.bizId,
    signer: current.value.confirmMode === 'SECOND_SIGN' ? signForm.signer : undefined
  });
  window.$message?.success($t('common.saveSuccess'));
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.confirm.title') }}</span>
          <PermissionGuard code="mes:confirm:add">
            <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
          </PermissionGuard>
        </div>
      </template>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showCreate" :title="$t('page.mes.confirm.addTitle')" class="w-560px" :on-confirm="submitCreate">
      <div class="mb-12px">{{ $t('page.mes.confirm.hint') }}</div>
      <NForm ref="createRef" :model="createForm" :rules="createRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.confirm.action')" path="actionCode">
          <NInput v-model:value="createForm.actionCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.confirm.mode')" path="confirmMode">
          <NSelect v-model:value="createForm.confirmMode" :options="modeOptions" />
        </NFormItem>
        <NFormItem v-if="createForm.confirmMode === 'SECOND_SIGN'" :label="$t('page.mes.confirm.role')" path="secondRoleCode">
          <NInput v-model:value="createForm.secondRoleCode" />
        </NFormItem>
      </NForm>
    </CrudDialog>
    <CrudDialog v-model:show="showSign" :title="$t('page.mes.confirm.signTitle')" class="w-560px" :on-confirm="submitSign">
      <div class="mb-12px">{{ current?.confirmMode === 'SECOND_SIGN' ? $t('page.mes.confirm.secondHint') : $t('page.mes.confirm.reauthHint') }}</div>
      <NForm ref="signRef" :model="signForm" :rules="signRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.confirm.bizType')" path="bizType">
          <NInput v-model:value="signForm.bizType" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.confirm.bizId')" path="bizId">
          <NInput v-model:value="signForm.bizId" />
        </NFormItem>
        <NFormItem v-if="current?.confirmMode === 'SECOND_SIGN'" :label="$t('page.mes.confirm.signer')" path="signer">
          <NInput v-model:value="signForm.signer" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
