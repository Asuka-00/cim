<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import type { DataTableColumns, FormInst, FormRules } from 'naive-ui';
import { createWarehouse, getWarehouses, type Warehouse } from '@/service/api/mes/location';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { $t } from '@/locales';

defineOptions({ name: 'MesWarehouse' });

const TYPES = ['RAW', 'LINESIDE', 'QUARANTINE', 'OTHER'];
const loading = ref(false);
const rows = ref<Warehouse[]>([]);
const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const form = reactive({ whCode: '', whName: '', whType: 'RAW' });
const rules = computed<FormRules>(() => ({
  whCode: [{ required: true, message: $t('page.mes.warehouse.required'), trigger: 'blur' }],
  whName: [{ required: true, message: $t('page.mes.warehouse.required'), trigger: 'blur' }]
}));
const typeOptions = TYPES.map(item => ({ label: item, value: item }));
const columns = computed<DataTableColumns<Warehouse>>(() => [
  { title: $t('page.mes.warehouse.code'), key: 'whCode', width: 160 },
  { title: $t('page.mes.warehouse.name'), key: 'whName' },
  { title: $t('page.mes.warehouse.type'), key: 'whType', width: 140 },
  {
    title: $t('page.mes.warehouse.enabled'),
    key: 'enabled',
    width: 90,
    render: row => (row.enabled === 0 ? $t('common.yesOrNo.no') : $t('common.yesOrNo.yes'))
  }
]);

async function fetchData() {
  loading.value = true;
  try {
    const res = await getWarehouses();
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  Object.assign(form, { whCode: '', whName: '', whType: 'RAW' });
  showModal.value = true;
}

async function submit() {
  await formRef.value?.validate();
  await createWarehouse({ whCode: form.whCode, whName: form.whName, whType: form.whType });
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
  return true;
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.warehouse.title') }}</span>
          <PermissionGuard code="mes:warehouse:add">
            <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
          </PermissionGuard>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.warehouse.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.warehouse.addTitle')" class="w-560px" :on-confirm="submit">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="110">
        <NFormItem :label="$t('page.mes.warehouse.code')" path="whCode">
          <NInput v-model:value="form.whCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.warehouse.name')" path="whName">
          <NInput v-model:value="form.whName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.warehouse.type')" path="whType">
          <NSelect v-model:value="form.whType" :options="typeOptions" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
