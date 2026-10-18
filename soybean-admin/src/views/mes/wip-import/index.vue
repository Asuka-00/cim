<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import type { FormInst, FormRules } from 'naive-ui';
import { getRunningImportTasks, importWipRow, importWipRows, type ImportTask } from '@/service/api/mes/import-task';
import { getStepList, type Step } from '@/service/api/mes/step';
import { getWorkOrderList, type WorkOrder } from '@/service/api/mes/work-order';
import { importWipLot } from '@/service/api/mes/lot';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { $t } from '@/locales';

defineOptions({ name: 'MesWipImport' });

const orders = ref<WorkOrder[]>([]);
const steps = ref<Step[]>([]);
const tasks = ref<ImportTask[]>([]);
const woId = ref<string | null>(null);
const taskId = ref<string | null>(null);
const batchText = ref('');
const woFormRef = ref<FormInst | null>(null);
const rowFormRef = ref<FormInst | null>(null);
const woForm = reactive({ lotNo: '', qty: null as number | null, seqNo: null as number | null, stepId: null as string | null });
const rowForm = reactive({
  rowNo: null as number | null,
  woId: null as string | null,
  lotNo: '',
  qty: null as number | null,
  seqNo: null as number | null,
  stepId: null as string | null
});
const rules = computed<FormRules>(() => ({
  lotNo: [{ required: true, message: $t('page.mes.wipImport.required'), trigger: 'blur' }],
  qty: [{ required: true, type: 'number', message: $t('page.mes.wipImport.required'), trigger: 'change' }],
  seqNo: [{ required: true, type: 'number', message: $t('page.mes.wipImport.required'), trigger: 'change' }],
  stepId: [{ required: true, message: $t('page.mes.wipImport.required'), trigger: 'change' }]
}));
const orderOptions = computed(() =>
  orders.value.filter(item => item.status === 'RELEASED').map(item => ({ label: item.woNo, value: String(item.id) }))
);
const stepOptions = computed(() => steps.value.map(item => ({ label: `${item.stepCode} ${item.stepName}`, value: String(item.id) })));
const taskOptions = computed(() => tasks.value.map(item => ({ label: `${item.taskNo} ${item.importType}`, value: String(item.id) })));

function rowBody(wo: string, lotNo: string, qty: number, seqNo: number, stepId: string) {
  return { lotNo, qty, seqNo, stepId };
}

async function submitWo() {
  await woFormRef.value?.validate();
  if (!woId.value || woForm.qty == null || woForm.qty <= 0 || woForm.seqNo == null || !woForm.stepId) return;
  await importWipLot(woId.value, {
    lotNo: woForm.lotNo,
    qty: woForm.qty,
    seqNo: woForm.seqNo,
    stepId: woForm.stepId
  });
  window.$message?.success($t('common.saveSuccess'));
}

async function submitRow() {
  await rowFormRef.value?.validate();
  if (!taskId.value || !rowForm.woId || rowForm.rowNo == null || rowForm.rowNo < 1 || rowForm.qty == null || rowForm.seqNo == null || !rowForm.stepId) {
    window.$message?.warning($t('page.mes.wipImport.rowInvalid'));
    return;
  }
  await importWipRow(taskId.value, {
    woId: rowForm.woId,
    rowNo: rowForm.rowNo,
    row: rowBody(rowForm.woId, rowForm.lotNo, rowForm.qty, rowForm.seqNo, rowForm.stepId)
  });
  window.$message?.success($t('common.saveSuccess'));
}

async function submitBatch() {
  if (!taskId.value) {
    window.$message?.warning($t('page.mes.wipImport.required'));
    return;
  }
  const rows = batchText.value
    .split('\n')
    .map(line => line.trim())
    .filter(Boolean)
    .map(line => {
      const [rowNo, wo, lotNo, qty, seqNo, stepId] = line.split(',');
      return {
        woId: wo?.trim(),
        rowNo: Number(rowNo),
        row: { lotNo: lotNo?.trim(), qty: Number(qty), seqNo: Number(seqNo), stepId: stepId?.trim() }
      };
    });
  if (!rows.length || rows.some(item => !item.woId || !item.row.lotNo || !item.row.stepId || item.rowNo < 1 || item.row.qty <= 0)) {
    window.$message?.warning($t('page.mes.wipImport.batchInvalid'));
    return;
  }
  await importWipRows(taskId.value, rows);
  window.$message?.success($t('common.saveSuccess'));
}

onMounted(async () => {
  const [orderPage, stepPage, running] = await Promise.all([
    getWorkOrderList({ page: 1, pageSize: 200 }),
    getStepList({ page: 1, pageSize: 200 }),
    getRunningImportTasks()
  ]);
  orders.value = orderPage.data?.records ?? [];
  steps.value = stepPage.data?.records ?? [];
  tasks.value = running.data ?? [];
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>{{ $t('page.mes.wipImport.title') }}</template>
      <div class="mb-12px">{{ $t('page.mes.wipImport.hint') }}</div>
      <div class="mb-8px font-medium">{{ $t('page.mes.wipImport.byOrder') }}</div>
      <NForm ref="woFormRef" :model="woForm" :rules="rules" label-placement="left" label-width="110" class="max-w-640px">
        <NFormItem :label="$t('page.mes.wipImport.order')">
          <NSelect v-model:value="woId" :options="orderOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.wipImport.lotNo')" path="lotNo">
          <NInput v-model:value="woForm.lotNo" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.wipImport.qty')" path="qty">
          <NInputNumber v-model:value="woForm.qty" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.wipImport.seq')" path="seqNo">
          <NInputNumber v-model:value="woForm.seqNo" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.wipImport.step')" path="stepId">
          <NSelect v-model:value="woForm.stepId" :options="stepOptions" filterable />
        </NFormItem>
        <PermissionGuard code="mes:wip-import:order">
          <NButton type="primary" :disabled="!woId" @click="submitWo">{{ $t('page.mes.wipImport.saveOrder') }}</NButton>
        </PermissionGuard>
      </NForm>
      <div class="mb-8px mt-24px font-medium">{{ $t('page.mes.wipImport.byTask') }}</div>
      <NForm ref="rowFormRef" :model="rowForm" :rules="rules" label-placement="left" label-width="110" class="max-w-640px">
        <NFormItem :label="$t('page.mes.wipImport.task')">
          <NSelect v-model:value="taskId" :options="taskOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.wipImport.rowNo')">
          <NInputNumber v-model:value="rowForm.rowNo" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.wipImport.order')">
          <NSelect v-model:value="rowForm.woId" :options="orderOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.wipImport.lotNo')" path="lotNo">
          <NInput v-model:value="rowForm.lotNo" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.wipImport.qty')" path="qty">
          <NInputNumber v-model:value="rowForm.qty" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.wipImport.seq')" path="seqNo">
          <NInputNumber v-model:value="rowForm.seqNo" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.wipImport.step')" path="stepId">
          <NSelect v-model:value="rowForm.stepId" :options="stepOptions" filterable />
        </NFormItem>
        <PermissionGuard code="mes:wip-import:row">
          <NButton type="primary" :disabled="!taskId" @click="submitRow">{{ $t('page.mes.wipImport.saveRow') }}</NButton>
        </PermissionGuard>
      </NForm>
      <div class="mb-8px mt-16px">{{ $t('page.mes.wipImport.batch') }}</div>
      <NInput v-model:value="batchText" type="textarea" :rows="6" class="max-w-640px" />
      <div class="mt-8px">
        <PermissionGuard code="mes:wip-import:batch">
          <NButton :disabled="!taskId" @click="submitBatch">{{ $t('page.mes.wipImport.saveBatch') }}</NButton>
        </PermissionGuard>
      </div>
    </NCard>
  </div>
</template>
