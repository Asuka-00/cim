<script setup lang="ts">
import { computed, h, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { createNotifyRule, disableNotifyRule, getNotifyRules, type NotifyRule } from '@/service/api/mes/notify-rule';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesNotifyRule' });

const LEVELS = ['INFO', 'WARN', 'ERROR'];
const RECEIVERS = ['ROLE', 'USER', 'AREA', 'ALL'];
const { hasAuth } = useAuth();
const loading = ref(false);
const eventType = ref('');
const rows = ref<NotifyRule[]>([]);
const showCreate = ref(false);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  ruleCode: '',
  ruleName: '',
  eventType: '',
  titleTemplate: '',
  contentTemplate: '',
  level: 'WARN',
  receiverType: 'ROLE',
  receiverId: ''
});

const rules = computed<FormRules>(() => ({
  ruleCode: [{ required: true, message: $t('page.mes.notify.required'), trigger: 'blur' }],
  ruleName: [{ required: true, message: $t('page.mes.notify.required'), trigger: 'blur' }],
  eventType: [{ required: true, message: $t('page.mes.notify.required'), trigger: 'blur' }],
  receiverId: form.receiverType === 'ALL' ? [] : [{ required: true, message: $t('page.mes.notify.required'), trigger: 'blur' }]
}));
const levelOptions = LEVELS.map(item => ({ label: item, value: item }));
const receiverOptions = RECEIVERS.map(item => ({ label: item, value: item }));

function openCreate() {
  Object.assign(form, {
    ruleCode: '',
    ruleName: '',
    eventType: eventType.value,
    titleTemplate: '',
    contentTemplate: '',
    level: 'WARN',
    receiverType: 'ROLE',
    receiverId: ''
  });
  showCreate.value = true;
}

const columns = computed<DataTableColumns<NotifyRule>>(() => [
  { title: $t('page.mes.notify.code'), key: 'ruleCode', width: 140 },
  { title: $t('page.mes.notify.name'), key: 'ruleName' },
  { title: $t('page.mes.notify.level'), key: 'level', width: 90 },
  { title: $t('page.mes.notify.receiver'), key: 'receiverType', width: 100 },
  { title: $t('page.mes.notify.receiverId'), key: 'receiverId' },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 90,
    render: row =>
      hasAuth('mes:notify:disable')
        ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => handleDisable(row) }, () =>
            $t('page.mes.notify.disable')
          )
        : null
  }
]);

async function fetchData() {
  if (!eventType.value.trim()) {
    rows.value = [];
    return;
  }
  loading.value = true;
  try {
    const res = await getNotifyRules(eventType.value.trim());
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function submitCreate() {
  await formRef.value?.validate();
  await createNotifyRule({
    ...form,
    receiverId: form.receiverType === 'ALL' ? undefined : form.receiverId
  });
  window.$message?.success($t('common.addSuccess'));
  eventType.value = form.eventType;
  await fetchData();
}

function handleDisable(row: NotifyRule) {
  window.$dialog?.warning({
    title: $t('page.mes.notify.disable'),
    content: $t('page.mes.notify.disableConfirm', { name: row.ruleCode }),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await disableNotifyRule(String(row.id));
      window.$message?.success($t('common.saveSuccess'));
      await fetchData();
    }
  });
}
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.notify.title') }}</span>
          <div class="flex items-center gap-8px">
            <NInput v-model:value="eventType" class="w-220px" :placeholder="$t('page.mes.notify.eventType')" @keyup.enter="fetchData" />
            <NButton @click="fetchData">{{ $t('common.search') }}</NButton>
            <PermissionGuard code="mes:notify:add">
              <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.notify.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showCreate" :title="$t('page.mes.notify.addTitle')" class="w-640px" :on-confirm="submitCreate">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.notify.code')" path="ruleCode">
          <NInput v-model:value="form.ruleCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.notify.name')" path="ruleName">
          <NInput v-model:value="form.ruleName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.notify.eventType')" path="eventType">
          <NInput v-model:value="form.eventType" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.notify.level')" path="level">
          <NSelect v-model:value="form.level" :options="levelOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.notify.receiver')" path="receiverType">
          <NSelect v-model:value="form.receiverType" :options="receiverOptions" />
        </NFormItem>
        <NFormItem v-if="form.receiverType !== 'ALL'" :label="$t('page.mes.notify.receiverId')" path="receiverId">
          <NInput v-model:value="form.receiverId" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.notify.titleTemplate')" path="titleTemplate">
          <NInput v-model:value="form.titleTemplate" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.notify.content')" path="contentTemplate">
          <NInput v-model:value="form.contentTemplate" type="textarea" :rows="2" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
