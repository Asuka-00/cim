<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { approveEcn, cancelEcn, createEcn, effectEcn, getEcns, rejectEcn, submitEcn, type Ecn } from '@/service/api/mes/ecn';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesEcn' });

const TYPES = ['ROUTE', 'RECIPE', 'BOM', 'PRODUCT', 'OTHER'];
const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<Ecn[]>([]);
const showCreate = ref(false);
const showDecide = ref(false);
const createRef = ref<FormInst | null>(null);
const decideRef = ref<FormInst | null>(null);
const current = ref<Ecn | null>(null);
const createForm = reactive({ ecnNo: '', title: '', changeType: 'OTHER', reason: '', applicant: '' });
const decideForm = reactive({ approver: '' });

const createRules = computed<FormRules>(() => ({
  ecnNo: [{ required: true, message: $t('page.mes.ecn.required'), trigger: 'blur' }],
  title: [{ required: true, message: $t('page.mes.ecn.required'), trigger: 'blur' }],
  changeType: [{ required: true, message: $t('page.mes.ecn.required'), trigger: 'change' }],
  reason: [{ required: true, message: $t('page.mes.ecn.required'), trigger: 'blur' }]
}));
const decideRules = computed<FormRules>(() => ({
  approver: [{ required: true, message: $t('page.mes.ecn.required'), trigger: 'blur' }]
}));
const typeOptions = TYPES.map(item => ({ label: item, value: item }));

function openCreate() {
  Object.assign(createForm, { ecnNo: '', title: '', changeType: 'OTHER', reason: '', applicant: '' });
  showCreate.value = true;
}

function openDecide(row: Ecn) {
  current.value = row;
  decideForm.approver = '';
  showDecide.value = true;
}

function ask(title: string, name: string, run: () => Promise<void>) {
  window.$dialog?.warning({
    title,
    content: name,
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: run
  });
}

const columns = computed<DataTableColumns<Ecn>>(() => [
  { title: $t('page.mes.ecn.no'), key: 'ecnNo', width: 140 },
  { title: $t('page.mes.ecn.titleText'), key: 'title' },
  { title: $t('page.mes.ecn.type'), key: 'changeType', width: 110 },
  { title: $t('page.mes.ecn.status'), key: 'status', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 260,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        row.status === 'DRAFT' && hasAuth('mes:ecn:submit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => ask($t('page.mes.ecn.submit'), row.ecnNo, async () => { await submitEcn(String(row.id)); window.$message?.success($t('common.saveSuccess')); await fetchData(); }) }, () => $t('page.mes.ecn.submit'))
          : null,
        row.status === 'DRAFT' && hasAuth('mes:ecn:cancel')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => ask($t('page.mes.ecn.cancel'), row.ecnNo, async () => { await cancelEcn(String(row.id)); window.$message?.success($t('common.saveSuccess')); await fetchData(); }) }, () => $t('page.mes.ecn.cancel'))
          : null,
        row.status === 'REVIEWING' && hasAuth('mes:ecn:decide')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openDecide(row) }, () => $t('page.mes.ecn.decide'))
          : null,
        row.status === 'APPROVED' && hasAuth('mes:ecn:effect')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => ask($t('page.mes.ecn.effect'), row.ecnNo, async () => { await effectEcn(String(row.id)); window.$message?.success($t('common.saveSuccess')); await fetchData(); }) }, () => $t('page.mes.ecn.effect'))
          : null
      ])
  }
]);

async function fetchData() {
  loading.value = true;
  try {
    const res = await getEcns();
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function submitCreate() {
  await createRef.value?.validate();
  await createEcn(createForm);
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
}

async function decide(pass: boolean) {
  await decideRef.value?.validate();
  if (!current.value) return false;
  if (pass) await approveEcn(String(current.value.id), { approver: decideForm.approver });
  else await rejectEcn(String(current.value.id), { approver: decideForm.approver });
  window.$message?.success($t('common.saveSuccess'));
  showDecide.value = false;
  await fetchData();
  return false;
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.ecn.pageTitle') }}</span>
          <PermissionGuard code="mes:ecn:add">
            <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
          </PermissionGuard>
        </div>
      </template>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showCreate" :title="$t('page.mes.ecn.addTitle')" class="w-640px" :on-confirm="submitCreate">
      <div class="mb-12px">{{ $t('page.mes.ecn.hint') }}</div>
      <NForm ref="createRef" :model="createForm" :rules="createRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.ecn.no')" path="ecnNo">
          <NInput v-model:value="createForm.ecnNo" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.ecn.titleText')" path="title">
          <NInput v-model:value="createForm.title" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.ecn.type')" path="changeType">
          <NSelect v-model:value="createForm.changeType" :options="typeOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.ecn.reason')" path="reason">
          <NInput v-model:value="createForm.reason" type="textarea" :rows="2" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.ecn.applicant')" path="applicant">
          <NInput v-model:value="createForm.applicant" />
        </NFormItem>
      </NForm>
    </CrudDialog>
    <CrudDialog v-model:show="showDecide" :title="$t('page.mes.ecn.decide')" class="w-480px" hide-footer>
      <NForm ref="decideRef" :model="decideForm" :rules="decideRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.ecn.approver')" path="approver">
          <NInput v-model:value="decideForm.approver" />
        </NFormItem>
      </NForm>
      <div class="mt-12px flex justify-end gap-8px">
        <NButton type="error" @click="decide(false)">{{ $t('page.mes.ecn.reject') }}</NButton>
        <NButton type="primary" @click="decide(true)">{{ $t('page.mes.ecn.approve') }}</NButton>
      </div>
    </CrudDialog>
  </div>
</template>
