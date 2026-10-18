<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getIncomingWafers, recordIncomingWafers, type IncomingWafer } from '@/service/api/mes/incoming-wafer';
import { getMaterialLots, type MaterialLot } from '@/service/api/mes/inspection';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesIncomingWafer' });

const { hasAuth } = useAuth();
const loading = ref(false);
const lots = ref<MaterialLot[]>([]);
const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const current = ref<MaterialLot | null>(null);
const existing = ref<IncomingWafer[]>([]);
const pending = ref<Array<{ vendorWaferId: string; slotNo: number | null }>>([]);
const form = reactive({ vendorWaferId: '', slotNo: null as number | null });

const rules = computed<FormRules>(() => ({
  vendorWaferId: [{ required: true, message: $t('page.mes.incomingWafer.required'), trigger: 'blur' }]
}));

const lotColumns = computed<DataTableColumns<MaterialLot>>(() => [
  { title: $t('page.mes.incomingWafer.lotCode'), key: 'lotCode' },
  { title: $t('page.mes.incomingWafer.status'), key: 'status', width: 130 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 100,
    render: row =>
      hasAuth('mes:incoming-wafer:add')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openLot(row) }, () =>
            $t('page.mes.incomingWafer.record')
          )
        : null
  }
]);

const waferColumns = computed<DataTableColumns<IncomingWafer>>(() => [
  { title: $t('page.mes.incomingWafer.vendorWaferId'), key: 'vendorWaferId' },
  { title: $t('page.mes.incomingWafer.slotNo'), key: 'slotNo', width: 90 },
  { title: $t('page.mes.incomingWafer.waferStatus'), key: 'status', width: 120 }
]);

async function openLot(row: MaterialLot) {
  current.value = row;
  pending.value = [];
  form.vendorWaferId = '';
  form.slotNo = null;
  const res = await getIncomingWafers(String(row.id));
  existing.value = res.data ?? [];
  showModal.value = true;
}

function addLine() {
  if (!form.vendorWaferId.trim()) return;
  pending.value = [...pending.value, { vendorWaferId: form.vendorWaferId.trim(), slotNo: form.slotNo }];
  form.vendorWaferId = '';
  form.slotNo = null;
}

async function handleSubmit() {
  if (!current.value || pending.value.length === 0) return false;
  await recordIncomingWafers(String(current.value.id), pending.value);
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

async function fetchData() {
  loading.value = true;
  try {
    const res = await getMaterialLots();
    lots.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <span>{{ $t('page.mes.incomingWafer.title') }}</span>
      </template>
      <DataTable flex-height :columns="lotColumns" :data="lots" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.incomingWafer.recordTitle')" class="w-640px" :on-confirm="handleSubmit">
      <DataTable :columns="waferColumns" :data="existing" size="small" class="mb-12px" />
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.incomingWafer.vendorWaferId')" path="vendorWaferId">
          <NInput v-model:value="form.vendorWaferId" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.incomingWafer.slotNo')" path="slotNo">
          <NInputNumber v-model:value="form.slotNo" :min="1" class="w-full" />
        </NFormItem>
        <NButton class="mb-12px" @click="addLine">{{ $t('page.mes.incomingWafer.addLine') }}</NButton>
      </NForm>
      <div v-if="pending.length">{{ pending.map(line => line.vendorWaferId).join('，') }}</div>
    </CrudDialog>
  </div>
</template>
