<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { ejectBatchLot, getBatchMembers, getBatches, type Batch, type BatchMember } from '@/service/api/mes/batch';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesBatchEject' });

const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<Batch[]>([]);
const lots = ref<Lot[]>([]);
const members = ref<BatchMember[]>([]);
const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const current = ref<Batch | null>(null);
const form = reactive({ lotId: null as string | null, reasonCode: '', remark: '' });

const rules = computed<FormRules>(() => ({
  lotId: [{ required: true, message: $t('page.mes.batchEject.required'), trigger: 'change' }],
  reasonCode: [{ required: true, message: $t('page.mes.batchEject.required'), trigger: 'blur' }]
}));

const lotName = computed(() => {
  const names = new Map(lots.value.map(item => [String(item.id), item.lotNo]));
  return (id: string) => names.get(String(id)) ?? id;
});
const lotOptions = computed(() => members.value.map(item => ({ label: lotName.value(String(item.lotId)), value: String(item.lotId) })));

async function openEject(row: Batch) {
  current.value = row;
  Object.assign(form, { lotId: null, reasonCode: '', remark: '' });
  const res = await getBatchMembers(String(row.id));
  members.value = res.data ?? [];
  showModal.value = true;
}

const columns = computed<DataTableColumns<Batch>>(() => [
  { title: $t('page.mes.batchEject.batchNo'), key: 'batchNo' },
  { title: $t('page.mes.batchEject.status'), key: 'status', width: 120 },
  { title: $t('page.mes.batchEject.lotCount'), key: 'lotCount', width: 100 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 100,
    render: row =>
      row.status === 'RUNNING' && hasAuth('mes:batch:eject')
        ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => openEject(row) }, () =>
            $t('page.mes.batchEject.eject')
          )
        : null
  }
]);

async function fetchData() {
  loading.value = true;
  try {
    const [batchRes, lotPage] = await Promise.all([getBatches(), getLotList({ page: 1, pageSize: 200, status: 'RUNNING' })]);
    rows.value = batchRes.data ?? [];
    lots.value = lotPage.data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

async function handleSubmit() {
  await formRef.value?.validate();
  if (!current.value || !form.lotId) return;
  await ejectBatchLot(String(current.value.id), form.lotId, { reasonCode: form.reasonCode, remark: form.remark });
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <span>{{ $t('page.mes.batchEject.title') }}</span>
      </template>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.batchEject.ejectTitle')" class="w-560px" :on-confirm="handleSubmit">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.batchEject.lot')" path="lotId">
          <NSelect v-model:value="form.lotId" :options="lotOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.batchEject.reasonCode')" path="reasonCode">
          <NInput v-model:value="form.reasonCode" />
        </NFormItem>
        <NFormItem :label="$t('common.remark')" path="remark">
          <NInput v-model:value="form.remark" type="textarea" :rows="2" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
