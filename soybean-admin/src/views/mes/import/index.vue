<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import {
  createImportTask,
  getImportTasksByType,
  importWipFile,
  startImportTask,
  submitImportTask,
  type ImportTask
} from '@/service/api/mes/import-task';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesImport' });

const { hasAuth } = useAuth();
const loading = ref(false);
const list = ref<ImportTask[]>([]);
const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  taskNo: '',
  mode: 'COMMIT',
  onError: 'SKIP',
  fileRef: ''
});

const rules = computed<FormRules>(() => ({
  taskNo: [{ required: true, message: $t('page.mes.importTask.required'), trigger: 'blur' }],
  fileRef: [{ required: true, message: $t('page.mes.importTask.required'), trigger: 'blur' }]
}));

const modeOptions = [
  { label: 'COMMIT', value: 'COMMIT' },
  { label: 'VALIDATE_ONLY', value: 'VALIDATE_ONLY' }
];
const onErrorOptions = [
  { label: 'SKIP', value: 'SKIP' },
  { label: 'ROLLBACK', value: 'ROLLBACK' }
];

async function fetchData() {
  loading.value = true;
  try {
    const res = await getImportTasksByType('WIP_INIT');
    list.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  Object.assign(form, { taskNo: '', mode: 'COMMIT', onError: 'SKIP', fileRef: '' });
  showModal.value = true;
}

async function handleCreate() {
  await formRef.value?.validate();
  await createImportTask({
    taskNo: form.taskNo,
    importType: 'WIP_INIT',
    mode: form.mode,
    onError: form.onError,
    fileRef: form.fileRef
  });
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
}

async function run(action: (id: string) => Promise<unknown>, row: ImportTask) {
  await action(String(row.id));
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

const columns = computed<DataTableColumns<ImportTask>>(() => [
  { title: $t('page.mes.importTask.taskNo'), key: 'taskNo' },
  { title: $t('page.mes.importTask.status'), key: 'status', width: 110 },
  { title: $t('page.mes.importTask.mode'), key: 'mode', width: 130 },
  { title: $t('page.mes.importTask.fileRef'), key: 'fileRef', width: 120 },
  { title: $t('page.mes.importTask.successRows'), key: 'successRows', width: 80 },
  { title: $t('page.mes.importTask.failRows'), key: 'failRows', width: 80 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 220,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        row.status === 'DRAFT' && hasAuth('mes:import:submit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => run(submitImportTask, row) }, () =>
              $t('page.mes.importTask.submit')
            )
          : null,
        row.status === 'PENDING' && hasAuth('mes:import:start')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => run(startImportTask, row) }, () =>
              $t('page.mes.importTask.start')
            )
          : null,
        row.status === 'RUNNING' && hasAuth('mes:import:run')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => run(importWipFile, row) }, () =>
              $t('page.mes.importTask.run')
            )
          : null
      ])
  }
]);

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.importTask.title') }}</span>
          <PermissionGuard code="mes:import:add">
            <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
          </PermissionGuard>
        </div>
      </template>
      <DataTable flex-height :columns="columns" :data="list" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.importTask.addTitle')" class="w-560px" :on-confirm="handleCreate">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.importTask.taskNo')" path="taskNo">
          <NInput v-model:value="form.taskNo" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.importTask.mode')" path="mode">
          <NSelect v-model:value="form.mode" :options="modeOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.importTask.onError')" path="onError">
          <NSelect v-model:value="form.onError" :options="onErrorOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.importTask.fileRef')" path="fileRef">
          <NInput v-model:value="form.fileRef" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
