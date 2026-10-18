<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getBatches, trackInBatch, type Batch } from '@/service/api/mes/batch';
import { getEquipmentList, type Equipment } from '@/service/api/mes/equipment';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesBatchTrackIn' });

const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<Batch[]>([]);
const equipment = ref<Equipment[]>([]);
const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const current = ref<Batch | null>(null);
const form = reactive({ equipmentId: null as string | null });

const rules = computed<FormRules>(() => ({
  equipmentId: [{ required: true, message: $t('page.mes.batchTrackIn.required'), trigger: 'change' }]
}));

const equipmentOptions = computed(() =>
  equipment.value.map(item => ({ label: `${item.equipmentCode} ${item.equipmentName}`, value: String(item.id) }))
);

function openTrackIn(row: Batch) {
  current.value = row;
  form.equipmentId = null;
  showModal.value = true;
}

const columns = computed<DataTableColumns<Batch>>(() => [
  { title: $t('page.mes.batchTrackIn.batchNo'), key: 'batchNo' },
  { title: $t('page.mes.batchTrackIn.status'), key: 'status', width: 120 },
  { title: $t('page.mes.batchTrackIn.lotCount'), key: 'lotCount', width: 100 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 100,
    render: row =>
      row.status === 'READY' && hasAuth('mes:batch:track-in')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openTrackIn(row) }, () =>
            $t('page.mes.batchTrackIn.trackIn')
          )
        : null
  }
]);

async function fetchData() {
  loading.value = true;
  try {
    const [batchRes, equipmentPage] = await Promise.all([getBatches(), getEquipmentList({ page: 1, pageSize: 200 })]);
    rows.value = batchRes.data ?? [];
    equipment.value = equipmentPage.data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

async function handleSubmit() {
  await formRef.value?.validate();
  if (!current.value || !form.equipmentId) return;
  await trackInBatch(String(current.value.id), { equipmentId: form.equipmentId });
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <span>{{ $t('page.mes.batchTrackIn.title') }}</span>
      </template>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.batchTrackIn.trackInTitle')" class="w-560px" :on-confirm="handleSubmit">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.batchTrackIn.equipment')" path="equipmentId">
          <NSelect v-model:value="form.equipmentId" :options="equipmentOptions" filterable />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
