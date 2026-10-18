<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import {
  createPrintTemplate,
  disablePrintTemplate,
  getPrintTemplates,
  setDefaultPrintTemplate,
  type PrintTemplate
} from '@/service/api/mes/print-template';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesPrintTemplate' });

const TYPES = ['LOT_TRAVELER', 'CARRIER_LABEL', 'WAFER_LABEL', 'STRIP_LABEL', 'REEL_LABEL', 'MATERIAL_LABEL'];
const { hasAuth } = useAuth();
const tplType = ref('LOT_TRAVELER');
const loading = ref(false);
const rows = ref<PrintTemplate[]>([]);
const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const form = reactive({ tplCode: '', tplName: '', content: '' });

const rules = computed<FormRules>(() => ({
  tplCode: [{ required: true, message: $t('page.mes.printTemplate.required'), trigger: 'blur' }],
  tplName: [{ required: true, message: $t('page.mes.printTemplate.required'), trigger: 'blur' }],
  content: [{ required: true, message: $t('page.mes.printTemplate.required'), trigger: 'blur' }]
}));
const typeOptions = TYPES.map(item => ({ label: item, value: item }));

function openCreate() {
  Object.assign(form, { tplCode: '', tplName: '', content: '' });
  showModal.value = true;
}

const columns = computed<DataTableColumns<PrintTemplate>>(() => [
  { title: $t('page.mes.printTemplate.code'), key: 'tplCode', width: 160 },
  { title: $t('page.mes.printTemplate.name'), key: 'tplName' },
  {
    title: $t('page.mes.printTemplate.defaultTpl'),
    key: 'isDefault',
    width: 90,
    render: row => (row.isDefault === 1 ? $t('common.yesOrNo.yes') : $t('common.yesOrNo.no'))
  },
  {
    title: $t('page.mes.printTemplate.enabled'),
    key: 'enabled',
    width: 90,
    render: row => (row.enabled === 0 ? $t('common.yesOrNo.no') : $t('common.yesOrNo.yes'))
  },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 180,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        row.enabled !== 0 && row.isDefault !== 1 && hasAuth('mes:print-template:default')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => makeDefault(row) }, () =>
              $t('page.mes.printTemplate.makeDefault')
            )
          : null,
        row.enabled !== 0 && hasAuth('mes:print-template:disable')
          ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => disable(row) }, () =>
              $t('page.mes.printTemplate.disable')
            )
          : null
      ])
  }
]);

async function fetchData() {
  loading.value = true;
  try {
    const res = await getPrintTemplates(tplType.value);
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function submit() {
  await formRef.value?.validate();
  await createPrintTemplate({
    tplCode: form.tplCode,
    tplName: form.tplName,
    tplType: tplType.value,
    content: form.content
  });
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
  return true;
}

async function makeDefault(row: PrintTemplate) {
  await setDefaultPrintTemplate(String(row.id));
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

async function disable(row: PrintTemplate) {
  await disablePrintTemplate(String(row.id));
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.printTemplate.title') }}</span>
          <div class="flex items-center gap-8px">
            <NSelect v-model:value="tplType" class="w-220px" :options="typeOptions" @update:value="fetchData" />
            <PermissionGuard code="mes:print-template:add">
              <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.printTemplate.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.printTemplate.addTitle')" class="w-640px" :on-confirm="submit">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="110">
        <NFormItem :label="$t('page.mes.printTemplate.code')" path="tplCode">
          <NInput v-model:value="form.tplCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.printTemplate.name')" path="tplName">
          <NInput v-model:value="form.tplName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.printTemplate.content')" path="content">
          <NInput v-model:value="form.content" type="textarea" :rows="6" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
