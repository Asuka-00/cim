<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { createShift, getShifts, type Shift } from '@/service/api/mes/shift';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { $t } from '@/locales';

defineOptions({ name: 'MesShift' });

const loading = ref(false);
const rows = ref<Shift[]>([]);
const showCreate = ref(false);
const formRef = ref<FormInst | null>(null);
const form = reactive({ shiftCode: '', shiftName: '', startTime: null as string | null, endTime: null as string | null });

const rules = computed<FormRules>(() => ({
  shiftCode: [{ required: true, message: $t('page.mes.shift.required'), trigger: 'blur' }],
  shiftName: [{ required: true, message: $t('page.mes.shift.required'), trigger: 'blur' }],
  startTime: [{ required: true, message: $t('page.mes.shift.required'), trigger: 'change' }],
  endTime: [{ required: true, message: $t('page.mes.shift.required'), trigger: 'change' }]
}));

function openCreate() {
  Object.assign(form, { shiftCode: '', shiftName: '', startTime: null, endTime: null });
  showCreate.value = true;
}

const columns = computed<DataTableColumns<Shift>>(() => [
  { title: $t('page.mes.shift.code'), key: 'shiftCode', width: 140 },
  { title: $t('page.mes.shift.name'), key: 'shiftName' },
  { title: $t('page.mes.shift.start'), key: 'startTime', width: 120 },
  { title: $t('page.mes.shift.end'), key: 'endTime', width: 120 },
  {
    title: $t('page.mes.shift.crossDay'),
    key: 'crossDay',
    width: 90,
    render: row => (row.crossDay === 1 ? $t('page.mes.shift.yes') : '')
  }
]);

async function fetchData() {
  loading.value = true;
  try {
    const res = await getShifts();
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function submitCreate() {
  await formRef.value?.validate();
  if (!form.startTime || !form.endTime) return;
  await createShift({
    shiftCode: form.shiftCode,
    shiftName: form.shiftName,
    startTime: form.startTime,
    endTime: form.endTime
  });
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.shift.title') }}</span>
          <PermissionGuard code="mes:shift:add">
            <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
          </PermissionGuard>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.shift.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showCreate" :title="$t('page.mes.shift.addTitle')" class="w-560px" :on-confirm="submitCreate">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.shift.code')" path="shiftCode">
          <NInput v-model:value="form.shiftCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.shift.name')" path="shiftName">
          <NInput v-model:value="form.shiftName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.shift.start')" path="startTime">
          <NTimePicker v-model:formatted-value="form.startTime" value-format="HH:mm:ss" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.shift.end')" path="endTime">
          <NTimePicker v-model:formatted-value="form.endTime" value-format="HH:mm:ss" class="w-full" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
