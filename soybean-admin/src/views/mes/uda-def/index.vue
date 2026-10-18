<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { createUdaDef, getUdaDefs, type UdaDef } from '@/service/api/mes/uda-def';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { $t } from '@/locales';

defineOptions({ name: 'MesUdaDef' });

const OBJECTS = ['LOT', 'WAFER', 'PRODUCT', 'EQUIPMENT', 'MATERIAL', 'WORK_ORDER'];
const TYPES = ['TEXT', 'NUMBER', 'DATE', 'BOOL', 'ENUM'];
const objectType = ref('LOT');
const loading = ref(false);
const rows = ref<UdaDef[]>([]);
const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const form = reactive({ attrCode: '', attrName: '', dataType: 'TEXT', options: '', required: false });

const rules = computed<FormRules>(() => ({
  attrCode: [{ required: true, message: $t('page.mes.udaDef.required'), trigger: 'blur' }],
  attrName: [{ required: true, message: $t('page.mes.udaDef.required'), trigger: 'blur' }]
}));
const objectOptions = OBJECTS.map(item => ({ label: item, value: item }));
const typeOptions = TYPES.map(item => ({ label: item, value: item }));
const columns = computed<DataTableColumns<UdaDef>>(() => [
  { title: $t('page.mes.udaDef.code'), key: 'attrCode', width: 160 },
  { title: $t('page.mes.udaDef.name'), key: 'attrName' },
  { title: $t('page.mes.udaDef.dataType'), key: 'dataType', width: 120 },
  { title: $t('page.mes.udaDef.options'), key: 'optionsJson' },
  {
    title: $t('page.mes.udaDef.requiredFlag'),
    key: 'required',
    width: 90,
    render: row => (row.required === 1 ? $t('common.yesOrNo.yes') : $t('common.yesOrNo.no'))
  }
]);

async function fetchData() {
  loading.value = true;
  try {
    const res = await getUdaDefs(objectType.value);
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  Object.assign(form, { attrCode: '', attrName: '', dataType: 'TEXT', options: '', required: false });
  showModal.value = true;
}

async function submit() {
  await formRef.value?.validate();
  if (form.dataType === 'ENUM' && !form.options.trim()) {
    window.$message?.warning($t('page.mes.udaDef.optionsRequired'));
    return false;
  }
  await createUdaDef({
    objectType: objectType.value,
    attrCode: form.attrCode,
    attrName: form.attrName,
    dataType: form.dataType,
    options: form.dataType === 'ENUM' ? form.options : undefined,
    required: form.required ? 1 : 0
  });
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
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.udaDef.title') }}</span>
          <div class="flex items-center gap-8px">
            <NSelect v-model:value="objectType" class="w-180px" :options="objectOptions" @update:value="fetchData" />
            <PermissionGuard code="mes:uda-def:add">
              <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.udaDef.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.udaDef.addTitle')" class="w-560px" :on-confirm="submit">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="110">
        <NFormItem :label="$t('page.mes.udaDef.code')" path="attrCode">
          <NInput v-model:value="form.attrCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.udaDef.name')" path="attrName">
          <NInput v-model:value="form.attrName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.udaDef.dataType')" path="dataType">
          <NSelect v-model:value="form.dataType" :options="typeOptions" />
        </NFormItem>
        <NFormItem v-if="form.dataType === 'ENUM'" :label="$t('page.mes.udaDef.options')" path="options">
          <NInput v-model:value="form.options" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.udaDef.requiredFlag')" path="required">
          <NSwitch v-model:value="form.required" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
