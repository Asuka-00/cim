<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst } from 'naive-ui';
import { getRouteList, type Route } from '@/service/api/mes/route';
import {
  approveRouteVersion,
  copyRouteVersion,
  createRouteVersion,
  getRouteVersionList,
  rejectRouteVersion,
  submitRouteVersion,
  updateRouteVersion,
  type RouteVersion
} from '@/service/api/mes/route-version';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesRouteVersion' });

const { hasAuth } = useAuth();
const routes = ref<Route[]>([]);
const routeId = ref<string | null>(null);
const actor = ref('');
const loading = ref(false);
const rows = ref<RouteVersion[]>([]);
const showModal = ref(false);
const editing = ref<RouteVersion | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({ remark: '' });

const routeOptions = computed(() => routes.value.map(item => ({ label: `${item.routeCode} ${item.routeName}`, value: String(item.id) })));

function openCreate() {
  editing.value = null;
  form.remark = '';
  showModal.value = true;
}

function openEdit(row: RouteVersion) {
  editing.value = row;
  form.remark = row.remark ?? '';
  showModal.value = true;
}

function requireActor() {
  if (!actor.value.trim()) {
    window.$message?.warning($t('page.mes.routeVersion.actorRequired'));
    return false;
  }
  return true;
}

const columns = computed<DataTableColumns<RouteVersion>>(() => [
  { title: $t('page.mes.routeVersion.versionNo'), key: 'versionNo', width: 90 },
  { title: $t('page.mes.routeVersion.status'), key: 'status', width: 110 },
  { title: $t('common.remark'), key: 'remark' },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 280,
    render: row =>
      h('div', { class: 'flex gap-8px flex-wrap' }, [
        hasAuth('mes:route-version:copy')
          ? h(NButton, { size: 'small', quaternary: true, onClick: () => copy(row) }, () => $t('page.mes.routeVersion.copy'))
          : null,
        row.status === 'DRAFT' && hasAuth('mes:route-version:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        row.status === 'DRAFT' && hasAuth('mes:route-version:submit')
          ? h(NButton, { size: 'small', quaternary: true, onClick: () => submit(row) }, () => $t('page.mes.routeVersion.submit'))
          : null,
        row.status === 'PENDING' && hasAuth('mes:route-version:approve')
          ? h(NButton, { size: 'small', quaternary: true, type: 'success', onClick: () => approve(row) }, () => $t('page.mes.routeVersion.approve'))
          : null,
        row.status === 'PENDING' && hasAuth('mes:route-version:approve')
          ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => reject(row) }, () => $t('page.mes.routeVersion.reject'))
          : null
      ])
  }
]);

async function fetchData() {
  if (!routeId.value) {
    rows.value = [];
    return;
  }
  loading.value = true;
  try {
    const res = await getRouteVersionList(routeId.value, { page: 1, pageSize: 50 });
    rows.value = res.data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

async function submitForm() {
  if (!routeId.value) return false;
  const data = { remark: form.remark };
  if (editing.value) await updateRouteVersion(routeId.value, String(editing.value.id), data);
  else await createRouteVersion(routeId.value, data);
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
  return true;
}

async function copy(row: RouteVersion) {
  if (!routeId.value) return;
  await copyRouteVersion(routeId.value, String(row.id));
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
}

async function submit(row: RouteVersion) {
  if (!routeId.value || !requireActor()) return;
  await submitRouteVersion(routeId.value, String(row.id), { assignee: actor.value.trim() });
  window.$message?.success($t('page.mes.routeVersion.submitted'));
  await fetchData();
}

async function approve(row: RouteVersion) {
  if (!routeId.value || !requireActor()) return;
  await approveRouteVersion(routeId.value, String(row.id), { operator: actor.value.trim() });
  window.$message?.success($t('page.mes.routeVersion.approved'));
  await fetchData();
}

async function reject(row: RouteVersion) {
  if (!routeId.value || !requireActor()) return;
  await rejectRouteVersion(routeId.value, String(row.id), { operator: actor.value.trim() });
  window.$message?.success($t('page.mes.routeVersion.rejected'));
  await fetchData();
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
          <span>{{ $t('page.mes.routeVersion.title') }}</span>
          <div class="flex items-center gap-8px">
            <NSelect v-model:value="routeId" class="w-240px" :options="routeOptions" filterable @update:value="fetchData" />
            <NInput v-model:value="actor" class="w-180px" :placeholder="$t('page.mes.routeVersion.actorHint')" />
            <PermissionGuard code="mes:route-version:add">
              <NButton type="primary" :disabled="!routeId" @click="openCreate">{{ $t('common.add') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.routeVersion.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog
      v-model:show="showModal"
      :title="editing ? $t('page.mes.routeVersion.editTitle') : $t('page.mes.routeVersion.addTitle')"
      class="w-560px"
      :on-confirm="submitForm"
    >
      <NForm ref="formRef" :model="form" label-placement="left" label-width="110">
        <NFormItem :label="$t('common.remark')" path="remark">
          <NInput v-model:value="form.remark" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
