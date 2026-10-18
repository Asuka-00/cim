<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getRouteList, type Route } from '@/service/api/mes/route';
import { getRouteStepList, type RouteStep } from '@/service/api/mes/route-step';
import {
  createRouteTimeLink,
  deleteRouteTimeLink,
  getRouteTimeLinkList,
  updateRouteTimeLink,
  type RouteTimeLink
} from '@/service/api/mes/route-time-link';
import { getRouteVersionList, type RouteVersion } from '@/service/api/mes/route-version';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesRouteTimeLink' });

const ACTIONS = ['HOLD', 'WARN'];
const { hasAuth } = useAuth();
const routes = ref<Route[]>([]);
const versions = ref<RouteVersion[]>([]);
const steps = ref<RouteStep[]>([]);
const routeId = ref<string | null>(null);
const versionId = ref<string | null>(null);
const loading = ref(false);
const rows = ref<RouteTimeLink[]>([]);
const showModal = ref(false);
const editing = ref<RouteTimeLink | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  fromSeqNo: null as number | null,
  toSeqNo: null as number | null,
  maxMinutes: null as number | null,
  warnMinutes: null as number | null,
  action: 'HOLD',
  reasonCode: ''
});

const formRules = computed<FormRules>(() => ({
  fromSeqNo: [{ required: true, type: 'number', message: $t('page.mes.routeTimeLink.required'), trigger: 'change' }],
  toSeqNo: [{ required: true, type: 'number', message: $t('page.mes.routeTimeLink.required'), trigger: 'change' }],
  maxMinutes: [{ required: true, type: 'number', message: $t('page.mes.routeTimeLink.required'), trigger: 'change' }]
}));
const routeOptions = computed(() => routes.value.map(item => ({ label: `${item.routeCode} ${item.routeName}`, value: String(item.id) })));
const versionOptions = computed(() => versions.value.map(item => ({ label: `${item.versionNo} ${item.status}`, value: String(item.id) })));
const seqOptions = computed(() => steps.value.map(item => ({ label: `${item.seqNo} ${item.stepAlias ?? ''}`.trim(), value: item.seqNo })));
const actionOptions = ACTIONS.map(item => ({ label: item, value: item }));
const draft = computed(() => versions.value.find(item => String(item.id) === versionId.value)?.status === 'DRAFT');

function openCreate() {
  editing.value = null;
  Object.assign(form, { fromSeqNo: null, toSeqNo: null, maxMinutes: null, warnMinutes: null, action: 'HOLD', reasonCode: '' });
  showModal.value = true;
}

function openEdit(row: RouteTimeLink) {
  editing.value = row;
  Object.assign(form, {
    fromSeqNo: row.fromSeqNo,
    toSeqNo: row.toSeqNo,
    maxMinutes: row.maxMinutes,
    warnMinutes: row.warnMinutes ?? null,
    action: row.action || 'HOLD',
    reasonCode: row.reasonCode ?? ''
  });
  showModal.value = true;
}

const columns = computed<DataTableColumns<RouteTimeLink>>(() => [
  { title: $t('page.mes.routeTimeLink.fromSeq'), key: 'fromSeqNo', width: 110 },
  { title: $t('page.mes.routeTimeLink.toSeq'), key: 'toSeqNo', width: 110 },
  { title: $t('page.mes.routeTimeLink.maxMinutes'), key: 'maxMinutes', width: 110 },
  { title: $t('page.mes.routeTimeLink.warnMinutes'), key: 'warnMinutes', width: 110 },
  { title: $t('page.mes.routeTimeLink.action'), key: 'action', width: 100 },
  { title: $t('page.mes.routeTimeLink.reasonCode'), key: 'reasonCode' },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 140,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        draft.value && hasAuth('mes:route-time-link:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        draft.value && hasAuth('mes:route-time-link:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);

async function onRouteChange(value: string | null) {
  versionId.value = null;
  versions.value = [];
  steps.value = [];
  rows.value = [];
  if (!value) return;
  const page = await getRouteVersionList(value, { page: 1, pageSize: 100 });
  versions.value = page.data?.records ?? [];
}

async function fetchData() {
  steps.value = [];
  rows.value = [];
  if (!versionId.value) return;
  loading.value = true;
  try {
    const [stepRes, linkRes] = await Promise.all([getRouteStepList(versionId.value), getRouteTimeLinkList(versionId.value)]);
    steps.value = stepRes.data ?? [];
    rows.value = linkRes.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function submit() {
  await formRef.value?.validate();
  if (!versionId.value || !draft.value) return false;
  if (form.fromSeqNo == null || form.toSeqNo == null || form.fromSeqNo >= form.toSeqNo || form.maxMinutes == null || form.maxMinutes <= 0) {
    window.$message?.warning($t('page.mes.routeTimeLink.seqInvalid'));
    return false;
  }
  if (form.warnMinutes != null && (form.warnMinutes <= 0 || form.warnMinutes >= form.maxMinutes)) {
    window.$message?.warning($t('page.mes.routeTimeLink.warnInvalid'));
    return false;
  }
  if (form.action === 'HOLD' && !form.reasonCode.trim()) {
    window.$message?.warning($t('page.mes.routeTimeLink.reasonRequired'));
    return false;
  }
  const data = {
    fromSeqNo: form.fromSeqNo,
    toSeqNo: form.toSeqNo,
    maxMinutes: form.maxMinutes,
    warnMinutes: form.warnMinutes,
    action: form.action,
    reasonCode: form.reasonCode.trim()
  };
  if (editing.value) await updateRouteTimeLink(versionId.value, String(editing.value.id), data);
  else await createRouteTimeLink(versionId.value, data);
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
  return true;
}

function handleDelete(row: RouteTimeLink) {
  if (!versionId.value) return;
  const version = versionId.value;
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.routeTimeLink.deleteConfirm', { from: String(row.fromSeqNo), to: String(row.toSeqNo) }),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteRouteTimeLink(version, String(row.id));
      window.$message?.success($t('common.saveSuccess'));
      await fetchData();
    }
  });
}

onMounted(async () => {
  const page = await getRouteList({ page: 1, pageSize: 200 });
  routes.value = page.data?.records ?? [];
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.routeTimeLink.title') }}</span>
          <div class="flex items-center gap-8px">
            <NSelect v-model:value="routeId" class="w-240px" :options="routeOptions" filterable @update:value="onRouteChange" />
            <NSelect v-model:value="versionId" class="w-180px" :options="versionOptions" :disabled="!routeId" @update:value="fetchData" />
            <PermissionGuard code="mes:route-time-link:add">
              <NButton type="primary" :disabled="!versionId || !draft" @click="openCreate">{{ $t('common.add') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.routeTimeLink.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog
      v-model:show="showModal"
      :title="editing ? $t('page.mes.routeTimeLink.editTitle') : $t('page.mes.routeTimeLink.addTitle')"
      class="w-560px"
      :on-confirm="submit"
    >
      <NForm ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="110">
        <NFormItem :label="$t('page.mes.routeTimeLink.fromSeq')" path="fromSeqNo">
          <NSelect v-model:value="form.fromSeqNo" :options="seqOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeTimeLink.toSeq')" path="toSeqNo">
          <NSelect v-model:value="form.toSeqNo" :options="seqOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeTimeLink.maxMinutes')" path="maxMinutes">
          <NInputNumber v-model:value="form.maxMinutes" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeTimeLink.warnMinutes')" path="warnMinutes">
          <NInputNumber v-model:value="form.warnMinutes" :min="1" clearable class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeTimeLink.action')" path="action">
          <NSelect v-model:value="form.action" :options="actionOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeTimeLink.reasonCode')" path="reasonCode">
          <NInput v-model:value="form.reasonCode" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
