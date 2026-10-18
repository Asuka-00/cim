<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getLotFiles, registerLotFile, storeLotFile, type MesFile } from '@/service/api/mes/file';
import { getLotList, type Lot } from '@/service/api/mes/lot';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { $t } from '@/locales';

defineOptions({ name: 'MesFile' });

const lots = ref<Lot[]>([]);
const lotId = ref<string | null>(null);
const loading = ref(false);
const rows = ref<MesFile[]>([]);
const showCreate = ref(false);
const formRef = ref<FormInst | null>(null);
const form = reactive({ fileName: '', content: '' });

const rules = computed<FormRules>(() => ({
  fileName: [{ required: true, message: $t('page.mes.file.required'), trigger: 'blur' }],
  content: [{ required: true, message: $t('page.mes.file.required'), trigger: 'blur' }]
}));
const lotOptions = computed(() => lots.value.map(item => ({ label: item.lotNo, value: String(item.id) })));

const columns = computed<DataTableColumns<MesFile>>(() => [
  { title: $t('page.mes.file.id'), key: 'id', width: 180 },
  { title: $t('page.mes.file.name'), key: 'fileName' },
  { title: $t('page.mes.file.size'), key: 'sizeBytes', width: 100 },
  { title: $t('page.mes.file.sha'), key: 'sha256' }
]);

async function sha256(bytes: Uint8Array) {
  const digest = await crypto.subtle.digest('SHA-256', bytes.slice().buffer);
  return [...new Uint8Array(digest)].map(item => item.toString(16).padStart(2, '0')).join('');
}

function openCreate() {
  Object.assign(form, { fileName: '', content: '' });
  showCreate.value = true;
}

async function fetchData() {
  if (!lotId.value) {
    rows.value = [];
    return;
  }
  loading.value = true;
  try {
    const res = await getLotFiles(lotId.value);
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function submitCreate() {
  await formRef.value?.validate();
  if (!lotId.value) return;
  const bytes = new TextEncoder().encode(form.content);
  const digest = await sha256(bytes);
  const before = new Set(rows.value.map(item => String(item.id)));
  await registerLotFile({
    fileName: form.fileName,
    contentType: 'text/plain',
    sizeBytes: bytes.byteLength,
    sha256: digest,
    lotId: lotId.value
  });
  const listed = await getLotFiles(lotId.value);
  const created = (listed.data ?? []).find(item => !before.has(String(item.id)) && item.sha256 === digest);
  if (created) {
    await storeLotFile(String(created.id), new Blob([bytes], { type: 'application/octet-stream' }));
  }
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
}

onMounted(async () => {
  const page = await getLotList({ page: 1, pageSize: 200 });
  lots.value = page.data?.records ?? [];
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.file.title') }}</span>
          <div class="flex items-center gap-8px">
            <NSelect v-model:value="lotId" class="w-240px" :options="lotOptions" filterable :placeholder="$t('page.mes.file.lot')" />
            <NButton @click="fetchData">{{ $t('common.search') }}</NButton>
            <PermissionGuard code="mes:file:add">
              <NButton type="primary" :disabled="!lotId" @click="openCreate">{{ $t('common.add') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.file.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showCreate" :title="$t('page.mes.file.addTitle')" class="w-640px" :on-confirm="submitCreate">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.file.name')" path="fileName">
          <NInput v-model:value="form.fileName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.file.content')" path="content">
          <NInput v-model:value="form.content" type="textarea" :rows="6" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
