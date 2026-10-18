<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { confirmHandover, createHandover, getHandovers, submitHandover, type Handover } from '@/service/api/mes/handover';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesHandover' });

const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<Handover[]>([]);
const showCreate = ref(false);
const showConfirm = ref(false);
const showSnapshot = ref(false);
const createRef = ref<FormInst | null>(null);
const confirmRef = ref<FormInst | null>(null);
const current = ref<Handover | null>(null);
const createForm = reactive({ handoverNo: '', fromUser: '', remark: '' });
const confirmForm = reactive({ toUser: '' });

const createRules = computed<FormRules>(() => ({
  handoverNo: [{ required: true, message: $t('page.mes.handover.required'), trigger: 'blur' }],
  fromUser: [{ required: true, message: $t('page.mes.handover.required'), trigger: 'blur' }]
}));
const confirmRules = computed<FormRules>(() => ({
  toUser: [{ required: true, message: $t('page.mes.handover.required'), trigger: 'blur' }]
}));

function openCreate() {
  Object.assign(createForm, { handoverNo: '', fromUser: '', remark: '' });
  showCreate.value = true;
}

function openConfirm(row: Handover) {
  current.value = row;
  confirmForm.toUser = '';
  showConfirm.value = true;
}

function openSnapshot(row: Handover) {
  current.value = row;
  showSnapshot.value = true;
}

const columns = computed<DataTableColumns<Handover>>(() => [
  { title: $t('page.mes.handover.no'), key: 'handoverNo', width: 160 },
  { title: $t('page.mes.handover.fromUser'), key: 'fromUser', width: 120 },
  { title: $t('page.mes.handover.toUser'), key: 'toUser', width: 120 },
  { title: $t('page.mes.handover.status'), key: 'status', width: 120 },
  { title: $t('common.remark'), key: 'remark' },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 220,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        row.status === 'DRAFT' && hasAuth('mes:handover:submit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => handleSubmit(row) }, () =>
              $t('page.mes.handover.submit')
            )
          : null,
        row.status === 'SUBMITTED' && hasAuth('mes:handover:confirm')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openConfirm(row) }, () =>
              $t('page.mes.handover.confirm')
            )
          : null,
        row.contentJson
          ? h(NButton, { size: 'small', quaternary: true, onClick: () => openSnapshot(row) }, () => $t('page.mes.handover.snapshot'))
          : null
      ])
  }
]);

async function fetchData() {
  loading.value = true;
  try {
    const res = await getHandovers();
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function submitCreate() {
  await createRef.value?.validate();
  await createHandover(createForm);
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
}

function handleSubmit(row: Handover) {
  window.$dialog?.warning({
    title: $t('page.mes.handover.submit'),
    content: $t('page.mes.handover.submitConfirm', { name: row.handoverNo }),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await submitHandover(String(row.id));
      window.$message?.success($t('common.saveSuccess'));
      await fetchData();
    }
  });
}

async function submitConfirm() {
  await confirmRef.value?.validate();
  if (!current.value) return;
  await confirmHandover(String(current.value.id), { toUser: confirmForm.toUser });
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.handover.title') }}</span>
          <PermissionGuard code="mes:handover:add">
            <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
          </PermissionGuard>
        </div>
      </template>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showCreate" :title="$t('page.mes.handover.addTitle')" class="w-560px" :on-confirm="submitCreate">
      <div class="mb-12px">{{ $t('page.mes.handover.hint') }}</div>
      <NForm ref="createRef" :model="createForm" :rules="createRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.handover.no')" path="handoverNo">
          <NInput v-model:value="createForm.handoverNo" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.handover.fromUser')" path="fromUser">
          <NInput v-model:value="createForm.fromUser" />
        </NFormItem>
        <NFormItem :label="$t('common.remark')" path="remark">
          <NInput v-model:value="createForm.remark" type="textarea" :rows="2" />
        </NFormItem>
      </NForm>
    </CrudDialog>
    <CrudDialog v-model:show="showConfirm" :title="$t('page.mes.handover.confirmTitle')" class="w-480px" :on-confirm="submitConfirm">
      <NForm ref="confirmRef" :model="confirmForm" :rules="confirmRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.handover.toUser')" path="toUser">
          <NInput v-model:value="confirmForm.toUser" />
        </NFormItem>
      </NForm>
    </CrudDialog>
    <CrudDialog v-model:show="showSnapshot" :title="$t('page.mes.handover.snapshot')" class="w-640px" hide-footer>
      <pre class="whitespace-pre-wrap">{{ current?.contentJson }}</pre>
    </CrudDialog>
  </div>
</template>
