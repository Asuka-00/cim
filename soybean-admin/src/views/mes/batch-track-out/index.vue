<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns } from 'naive-ui';
import { getBatches, trackOutBatch, type Batch } from '@/service/api/mes/batch';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesBatchTrackOut' });

const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<Batch[]>([]);
const showModal = ref(false);
const current = ref<Batch | null>(null);
const form = reactive({ remark: '' });

function openTrackOut(row: Batch) {
  current.value = row;
  form.remark = '';
  showModal.value = true;
}

const columns = computed<DataTableColumns<Batch>>(() => [
  { title: $t('page.mes.batchTrackOut.batchNo'), key: 'batchNo' },
  { title: $t('page.mes.batchTrackOut.status'), key: 'status', width: 120 },
  { title: $t('page.mes.batchTrackOut.lotCount'), key: 'lotCount', width: 100 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 100,
    render: row =>
      row.status === 'RUNNING' && hasAuth('mes:batch:track-out')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openTrackOut(row) }, () =>
            $t('page.mes.batchTrackOut.trackOut')
          )
        : null
  }
]);

async function fetchData() {
  loading.value = true;
  try {
    const res = await getBatches();
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function handleSubmit() {
  if (!current.value) return;
  await trackOutBatch(String(current.value.id), { remark: form.remark });
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <span>{{ $t('page.mes.batchTrackOut.title') }}</span>
      </template>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.batchTrackOut.trackOutTitle')" class="w-560px" :on-confirm="handleSubmit">
      <NForm :model="form" label-placement="left" label-width="96">
        <NFormItem :label="$t('common.remark')" path="remark">
          <NInput v-model:value="form.remark" type="textarea" :rows="2" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
