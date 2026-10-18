<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { bindCarrier, createCarrier, getCarriers, unbindCarrier, type Carrier } from '@/service/api/mes/carrier';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesCarrier' });

const TYPES = ['FOUP', 'CASSETTE', 'MAGAZINE', 'OTHER'];
const OPEN = new Set(['WAITING', 'RUNNING']);
const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<Carrier[]>([]);
const lots = ref<Lot[]>([]);
const showCreate = ref(false);
const showBind = ref(false);
const createRef = ref<FormInst | null>(null);
const bindRef = ref<FormInst | null>(null);
const current = ref<Carrier | null>(null);
const createForm = reactive({ carrierCode: '', carrierType: 'FOUP', capacity: 25 });
const bindForm = reactive({ lotId: null as string | null });

const createRules = computed<FormRules>(() => ({
  carrierCode: [{ required: true, message: $t('page.mes.carrier.required'), trigger: 'blur' }]
}));
const bindRules = computed<FormRules>(() => ({
  lotId: [{ required: true, message: $t('page.mes.carrier.required'), trigger: 'change' }]
}));

const typeOptions = TYPES.map(item => ({ label: item, value: item }));
const lotName = computed(() => {
  const names = new Map(lots.value.map(item => [String(item.id), item.lotNo]));
  return (id?: string | null) => (id ? (names.get(String(id)) ?? id) : '');
});
const lotOptions = computed(() =>
  lots.value.filter(item => OPEN.has(item.status)).map(item => ({ label: item.lotNo, value: String(item.id) }))
);

function openCreate() {
  Object.assign(createForm, { carrierCode: '', carrierType: 'FOUP', capacity: 25 });
  showCreate.value = true;
}

function openBind(row: Carrier) {
  current.value = row;
  bindForm.lotId = null;
  showBind.value = true;
}

const columns = computed<DataTableColumns<Carrier>>(() => [
  { title: $t('page.mes.carrier.code'), key: 'carrierCode' },
  { title: $t('page.mes.carrier.type'), key: 'carrierType', width: 120 },
  { title: $t('page.mes.carrier.status'), key: 'status', width: 110 },
  { title: $t('page.mes.carrier.lot'), key: 'currentLotId', render: row => lotName.value(row.currentLotId) },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 140,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        row.status === 'EMPTY' && hasAuth('mes:carrier:bind')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openBind(row) }, () => $t('page.mes.carrier.bind'))
          : null,
        row.status === 'OCCUPIED' && hasAuth('mes:carrier:unbind')
          ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => handleUnbind(row) }, () =>
              $t('page.mes.carrier.unbind')
            )
          : null
      ])
  }
]);

async function fetchData() {
  loading.value = true;
  try {
    const [carrierRes, lotPage] = await Promise.all([getCarriers(), getLotList({ page: 1, pageSize: 200 })]);
    rows.value = carrierRes.data ?? [];
    lots.value = lotPage.data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

async function submitCreate() {
  await createRef.value?.validate();
  await createCarrier(createForm);
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
}

async function submitBind() {
  await bindRef.value?.validate();
  if (!current.value || !bindForm.lotId) return;
  await bindCarrier(String(current.value.id), bindForm.lotId);
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

function handleUnbind(row: Carrier) {
  window.$dialog?.warning({
    title: $t('page.mes.carrier.unbind'),
    content: $t('page.mes.carrier.unbindConfirm', { name: row.carrierCode }),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await unbindCarrier(String(row.id));
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
          <span>{{ $t('page.mes.carrier.title') }}</span>
          <PermissionGuard code="mes:carrier:add">
            <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
          </PermissionGuard>
        </div>
      </template>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showCreate" :title="$t('page.mes.carrier.addTitle')" class="w-560px" :on-confirm="submitCreate">
      <NForm ref="createRef" :model="createForm" :rules="createRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.carrier.code')" path="carrierCode">
          <NInput v-model:value="createForm.carrierCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.carrier.type')" path="carrierType">
          <NSelect v-model:value="createForm.carrierType" :options="typeOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.carrier.capacity')" path="capacity">
          <NInputNumber v-model:value="createForm.capacity" :min="1" class="w-full" />
        </NFormItem>
      </NForm>
    </CrudDialog>
    <CrudDialog v-model:show="showBind" :title="$t('page.mes.carrier.bindTitle')" class="w-560px" :on-confirm="submitBind">
      <NForm ref="bindRef" :model="bindForm" :rules="bindRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.carrier.lot')" path="lotId">
          <NSelect v-model:value="bindForm.lotId" :options="lotOptions" filterable />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
