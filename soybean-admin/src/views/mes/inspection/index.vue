<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getMaterialLots, inspectMaterialLot, type MaterialLot } from '@/service/api/mes/inspection';
import { getMaterialList, type Material } from '@/service/api/mes/material';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesInspection' });

const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<MaterialLot[]>([]);
const materials = ref<Material[]>([]);
const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const current = ref<MaterialLot | null>(null);
const form = reactive({ result: 'PASS', remark: '' });

const resultOptions = [
  { label: 'PASS', value: 'PASS' },
  { label: 'FAIL', value: 'FAIL' },
  { label: 'CONCESSION', value: 'CONCESSION' }
];

const rules = computed<FormRules>(() => ({
  result: [{ required: true, message: $t('page.mes.inspection.required'), trigger: 'change' }],
  remark:
    form.result === 'CONCESSION'
      ? [{ required: true, message: $t('page.mes.inspection.concessionRequired'), trigger: 'blur' }]
      : []
}));

const materialName = computed(() => {
  const names = new Map(materials.value.map(item => [String(item.id), `${item.materialCode} ${item.materialName}`]));
  return (id: string) => names.get(String(id)) ?? id;
});

function openInspect(row: MaterialLot) {
  current.value = row;
  form.result = 'PASS';
  form.remark = '';
  showModal.value = true;
}

const columns = computed<DataTableColumns<MaterialLot>>(() => [
  { title: $t('page.mes.inspection.lotCode'), key: 'lotCode' },
  { title: $t('page.mes.inspection.material'), key: 'materialId', render: row => materialName.value(row.materialId) },
  { title: $t('page.mes.inspection.status'), key: 'status', width: 130 },
  { title: $t('page.mes.inspection.qcResult'), key: 'qcResult', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 100,
    render: row =>
      row.status === 'PENDING_QC' && hasAuth('mes:material-lot:inspect')
        ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openInspect(row) }, () =>
            $t('page.mes.inspection.inspect')
          )
        : null
  }
]);

async function fetchData() {
  loading.value = true;
  try {
    const [lotRes, materialPage] = await Promise.all([getMaterialLots(), getMaterialList({ page: 1, pageSize: 200 })]);
    rows.value = lotRes.data ?? [];
    materials.value = materialPage.data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

async function handleSubmit() {
  await formRef.value?.validate();
  if (!current.value) return;
  await inspectMaterialLot(String(current.value.id), { result: form.result, remark: form.remark });
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <span>{{ $t('page.mes.inspection.title') }}</span>
      </template>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.inspection.inspectTitle')" class="w-560px" :on-confirm="handleSubmit">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.inspection.result')" path="result">
          <NSelect v-model:value="form.result" :options="resultOptions" />
        </NFormItem>
        <NFormItem :label="$t('common.remark')" path="remark">
          <NInput v-model:value="form.remark" type="textarea" :rows="2" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
