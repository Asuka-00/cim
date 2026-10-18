<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import type { FormInst, FormRules } from 'naive-ui';
import { getRunningImportTasks, recordImportFailure, type ImportTask } from '@/service/api/mes/import-task';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { $t } from '@/locales';

defineOptions({ name: 'MesImportFail' });

const tasks = ref<ImportTask[]>([]);
const taskId = ref<string | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({ rowNo: null as number | null, message: '', errorCode: '', rawData: '' });
const rules = computed<FormRules>(() => ({
  rowNo: [{ required: true, type: 'number', message: $t('page.mes.importFail.required'), trigger: 'change' }],
  message: [{ required: true, message: $t('page.mes.importFail.required'), trigger: 'blur' }]
}));
const taskOptions = computed(() => tasks.value.map(item => ({ label: `${item.taskNo} ${item.importType}`, value: String(item.id) })));

async function loadTasks() {
  const res = await getRunningImportTasks();
  tasks.value = res.data ?? [];
}

async function submit() {
  await formRef.value?.validate();
  if (!taskId.value || form.rowNo == null || form.rowNo < 1) {
    window.$message?.warning($t('page.mes.importFail.rowInvalid'));
    return;
  }
  if (form.message.length > 512 || form.errorCode.length > 32 || form.rawData.length > 1024) {
    window.$message?.warning($t('page.mes.importFail.lengthInvalid'));
    return;
  }
  await recordImportFailure(taskId.value, {
    rowNo: form.rowNo,
    message: form.message,
    errorCode: form.errorCode.trim() || undefined,
    rawData: form.rawData.trim() || undefined
  });
  window.$message?.success($t('common.saveSuccess'));
  Object.assign(form, { rowNo: null, message: '', errorCode: '', rawData: '' });
  await loadTasks();
}

onMounted(loadTasks);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.importFail.title') }}</span>
          <NSelect v-model:value="taskId" class="w-280px" :options="taskOptions" filterable />
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.importFail.hint') }}</div>
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="110" class="max-w-640px">
        <NFormItem :label="$t('page.mes.importFail.rowNo')" path="rowNo">
          <NInputNumber v-model:value="form.rowNo" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.importFail.message')" path="message">
          <NInput v-model:value="form.message" type="textarea" :rows="3" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.importFail.code')" path="errorCode">
          <NInput v-model:value="form.errorCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.importFail.raw')" path="rawData">
          <NInput v-model:value="form.rawData" type="textarea" :rows="3" />
        </NFormItem>
        <PermissionGuard code="mes:import:fail">
          <NButton type="primary" :disabled="!taskId" @click="submit">{{ $t('page.mes.importFail.save') }}</NButton>
        </PermissionGuard>
      </NForm>
    </NCard>
  </div>
</template>
