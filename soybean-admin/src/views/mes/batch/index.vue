<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import {
  addBatchLot,
  cancelBatch,
  createBatch,
  getBatchMembers,
  getBatches,
  removeBatchLot,
  type Batch,
  type BatchMember
} from '@/service/api/mes/batch';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import { getStepList, type Step } from '@/service/api/mes/step';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesBatch' });

const OPEN = new Set(['OPEN', 'READY']);
const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<Batch[]>([]);
const steps = ref<Step[]>([]);
const lots = ref<Lot[]>([]);
const showCreate = ref(false);
const showAdd = ref(false);
const showMembers = ref(false);
const createRef = ref<FormInst | null>(null);
const addRef = ref<FormInst | null>(null);
const current = ref<Batch | null>(null);
const members = ref<BatchMember[]>([]);
const createForm = reactive({ batchNo: '', stepId: null as string | null, minLots: 1, maxLots: 1 });
const addForm = reactive({ lotId: null as string | null });

const createRules = computed<FormRules>(() => ({
  batchNo: [{ required: true, message: $t('page.mes.batch.required'), trigger: 'blur' }],
  stepId: [{ required: true, message: $t('page.mes.batch.required'), trigger: 'change' }],
  maxLots: [{ required: true, type: 'number', message: $t('page.mes.batch.required'), trigger: 'blur' }]
}));
const addRules = computed<FormRules>(() => ({
  lotId: [{ required: true, message: $t('page.mes.batch.required'), trigger: 'change' }]
}));

const stepOptions = computed(() => steps.value.map(item => ({ label: `${item.stepCode} ${item.stepName}`, value: String(item.id) })));
const lotName = computed(() => {
  const names = new Map(lots.value.map(item => [String(item.id), item.lotNo]));
  return (id: string) => names.get(String(id)) ?? id;
});
const lotOptions = computed(() =>
  lots.value
    .filter(item => item.status === 'WAITING' && (item.holdCount ?? 0) === 0)
    .map(item => ({ label: item.lotNo, value: String(item.id) }))
);

function openCreate() {
  Object.assign(createForm, { batchNo: '', stepId: null, minLots: 1, maxLots: 1 });
  showCreate.value = true;
}

function openAdd(row: Batch) {
  current.value = row;
  addForm.lotId = null;
  showAdd.value = true;
}

async function openMembers(row: Batch) {
  current.value = row;
  const res = await getBatchMembers(String(row.id));
  members.value = res.data ?? [];
  showMembers.value = true;
}

async function fetchData() {
  loading.value = true;
  try {
    const [batchRes, stepPage, lotPage] = await Promise.all([
      getBatches(),
      getStepList({ page: 1, pageSize: 200 }),
      getLotList({ page: 1, pageSize: 200, status: 'WAITING' })
    ]);
    rows.value = batchRes.data ?? [];
    steps.value = stepPage.data?.records ?? [];
    lots.value = lotPage.data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

const columns = computed<DataTableColumns<Batch>>(() => [
  { title: $t('page.mes.batch.batchNo'), key: 'batchNo' },
  { title: $t('page.mes.batch.status'), key: 'status', width: 110 },
  { title: $t('page.mes.batch.lotCount'), key: 'lotCount', width: 90 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 220,
    render: row =>
      OPEN.has(row.status)
        ? h('div', { class: 'flex gap-8px' }, [
            hasAuth('mes:batch:member')
              ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openAdd(row) }, () => $t('page.mes.batch.add'))
              : null,
            hasAuth('mes:batch:member')
              ? h(NButton, { size: 'small', quaternary: true, onClick: () => openMembers(row) }, () => $t('page.mes.batch.members'))
              : null,
            hasAuth('mes:batch:cancel')
              ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleCancel(row) }, () =>
                  $t('page.mes.batch.cancel')
                )
              : null
          ])
        : null
  }
]);

const memberColumns = computed<DataTableColumns<BatchMember>>(() => [
  { title: $t('page.mes.batch.lot'), key: 'lotId', render: row => lotName.value(row.lotId) },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 90,
    render: row =>
      h(
        NButton,
        { size: 'small', quaternary: true, type: 'error', onClick: () => handleRemove(row) },
        () => $t('page.mes.batch.remove')
      )
  }
]);

async function submitCreate() {
  await createRef.value?.validate();
  if (!createForm.stepId) return;
  await createBatch({
    batchNo: createForm.batchNo,
    stepId: createForm.stepId,
    minLots: createForm.minLots,
    maxLots: createForm.maxLots
  });
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
}

async function submitAdd() {
  await addRef.value?.validate();
  if (!current.value || !addForm.lotId) return;
  await addBatchLot(String(current.value.id), addForm.lotId);
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

async function handleRemove(row: BatchMember) {
  if (!current.value) return;
  await removeBatchLot(String(current.value.id), String(row.lotId));
  window.$message?.success($t('common.saveSuccess'));
  const res = await getBatchMembers(String(current.value.id));
  members.value = res.data ?? [];
  await fetchData();
}

function handleCancel(row: Batch) {
  window.$dialog?.warning({
    title: $t('page.mes.batch.cancel'),
    content: $t('page.mes.batch.cancelConfirm', { name: row.batchNo }),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await cancelBatch(String(row.id));
      window.$message?.success($t('common.saveSuccess'));
      await fetchData();
    }
  });
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.batch.title') }}</span>
          <PermissionGuard code="mes:batch:add">
            <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
          </PermissionGuard>
        </div>
      </template>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showCreate" :title="$t('page.mes.batch.addTitle')" class="w-560px" :on-confirm="submitCreate">
      <NForm ref="createRef" :model="createForm" :rules="createRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.batch.batchNo')" path="batchNo">
          <NInput v-model:value="createForm.batchNo" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.batch.step')" path="stepId">
          <NSelect v-model:value="createForm.stepId" :options="stepOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.batch.minLots')" path="minLots">
          <NInputNumber v-model:value="createForm.minLots" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.batch.maxLots')" path="maxLots">
          <NInputNumber v-model:value="createForm.maxLots" :min="1" class="w-full" />
        </NFormItem>
      </NForm>
    </CrudDialog>
    <CrudDialog v-model:show="showAdd" :title="$t('page.mes.batch.addLotTitle')" class="w-560px" :on-confirm="submitAdd">
      <NForm ref="addRef" :model="addForm" :rules="addRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.batch.lot')" path="lotId">
          <NSelect v-model:value="addForm.lotId" :options="lotOptions" filterable />
        </NFormItem>
      </NForm>
    </CrudDialog>
    <CrudDialog v-model:show="showMembers" :title="$t('page.mes.batch.members')" class="w-560px" hide-footer>
      <DataTable :columns="memberColumns" :data="members" size="small" />
    </CrudDialog>
  </div>
</template>
