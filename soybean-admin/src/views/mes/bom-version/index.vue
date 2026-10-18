<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst } from 'naive-ui';
import { getBomList, type Bom } from '@/service/api/mes/bom';
import {
  approveBomVersion,
  copyBomVersion,
  createBomVersion,
  getBomVersionList,
  rejectBomVersion,
  submitBomVersion,
  updateBomVersion,
  type BomVersion
} from '@/service/api/mes/bom-version';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesBomVersion' });

const { hasAuth } = useAuth();
const boms = ref<Bom[]>([]);
const bomId = ref<string | null>(null);
const actor = ref('');
const loading = ref(false);
const rows = ref<BomVersion[]>([]);
const showModal = ref(false);
const editing = ref<BomVersion | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({ remark: '' });

const bomOptions = computed(() => boms.value.map(item => ({ label: `${item.bomCode} ${item.bomName}`, value: String(item.id) })));

function openCreate() {
  editing.value = null;
  form.remark = '';
  showModal.value = true;
}

function openEdit(row: BomVersion) {
  editing.value = row;
  form.remark = row.remark ?? '';
  showModal.value = true;
}

function requireActor() {
  if (!actor.value.trim()) {
    window.$message?.warning($t('page.mes.bomVersion.actorRequired'));
    return false;
  }
  return true;
}

const columns = computed<DataTableColumns<BomVersion>>(() => [
  { title: $t('page.mes.bomVersion.versionNo'), key: 'versionNo', width: 90 },
  { title: $t('page.mes.bomVersion.status'), key: 'status', width: 110 },
  { title: $t('common.remark'), key: 'remark' },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 280,
    render: row =>
      h('div', { class: 'flex gap-8px flex-wrap' }, [
        hasAuth('mes:bom-version:copy')
          ? h(NButton, { size: 'small', quaternary: true, onClick: () => copy(row) }, () => $t('page.mes.bomVersion.copy'))
          : null,
        row.status === 'DRAFT' && hasAuth('mes:bom-version:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        row.status === 'DRAFT' && hasAuth('mes:bom-version:submit')
          ? h(NButton, { size: 'small', quaternary: true, onClick: () => submit(row) }, () => $t('page.mes.bomVersion.submit'))
          : null,
        row.status === 'PENDING' && hasAuth('mes:bom-version:approve')
          ? h(NButton, { size: 'small', quaternary: true, type: 'success', onClick: () => approve(row) }, () => $t('page.mes.bomVersion.approve'))
          : null,
        row.status === 'PENDING' && hasAuth('mes:bom-version:approve')
          ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => reject(row) }, () => $t('page.mes.bomVersion.reject'))
          : null
      ])
  }
]);

async function fetchData() {
  if (!bomId.value) {
    rows.value = [];
    return;
  }
  loading.value = true;
  try {
    const res = await getBomVersionList(bomId.value, { page: 1, pageSize: 50 });
    rows.value = res.data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

async function submitForm() {
  if (!bomId.value) return false;
  const data = { remark: form.remark };
  if (editing.value) await updateBomVersion(bomId.value, String(editing.value.id), data);
  else await createBomVersion(bomId.value, data);
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
  return true;
}

async function copy(row: BomVersion) {
  if (!bomId.value) return;
  await copyBomVersion(bomId.value, String(row.id));
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
}

async function submit(row: BomVersion) {
  if (!bomId.value || !requireActor()) return;
  await submitBomVersion(bomId.value, String(row.id), { assignee: actor.value.trim() });
  window.$message?.success($t('page.mes.bomVersion.submitted'));
  await fetchData();
}

async function approve(row: BomVersion) {
  if (!bomId.value || !requireActor()) return;
  await approveBomVersion(bomId.value, String(row.id), { operator: actor.value.trim() });
  window.$message?.success($t('page.mes.bomVersion.approved'));
  await fetchData();
}

async function reject(row: BomVersion) {
  if (!bomId.value || !requireActor()) return;
  await rejectBomVersion(bomId.value, String(row.id), { operator: actor.value.trim() });
  window.$message?.success($t('page.mes.bomVersion.rejected'));
  await fetchData();
}

onMounted(async () => {
  const page = await getBomList({ page: 1, pageSize: 200 });
  boms.value = page.data?.records ?? [];
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.bomVersion.title') }}</span>
          <div class="flex items-center gap-8px">
            <NSelect v-model:value="bomId" class="w-240px" :options="bomOptions" filterable @update:value="fetchData" />
            <NInput v-model:value="actor" class="w-180px" :placeholder="$t('page.mes.bomVersion.actorHint')" />
            <PermissionGuard code="mes:bom-version:add">
              <NButton type="primary" :disabled="!bomId" @click="openCreate">{{ $t('common.add') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.bomVersion.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog
      v-model:show="showModal"
      :title="editing ? $t('page.mes.bomVersion.editTitle') : $t('page.mes.bomVersion.addTitle')"
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
