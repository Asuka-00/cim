<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue';
import { NButton, type DataTableColumns } from 'naive-ui';
import {
  downloadLogFile,
  getLogFileList,
  getLogFileTail,
  type LogFileInfoVO,
  type LogFileType,
  type LogPreviewLineCount
} from '@/service/api/rtd/log-management/log-file';
import PermissionGuard from '@/components/business/permission-guard.vue';
import LargeViewer from '@/components/business/large-viewer.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';
import { renderBoolTag, renderTag } from '@/utils/tag-render';

defineOptions({ name: 'RtdLogManagementLogFile' });

const { hasAuth } = useAuth();

const TYPES: { label: string; value: LogFileType }[] = [
  { label: 'app', value: 'app' },
  { label: 'audit', value: 'audit' },
  { label: 'sql', value: 'sql' },
  { label: 'error', value: 'error' },
  { label: 'slow', value: 'slow' }
];

const loading = ref(false);
const list = ref<LogFileInfoVO[]>([]);
const fileType = ref<LogFileType>('app');
const showPreview = ref(false);
const previewLoading = ref(false);
const previewContent = ref<string[]>([]);
const previewFile = ref<LogFileInfoVO | null>(null);
const previewLines = ref<LogPreviewLineCount>(500);

const columns = computed<DataTableColumns<LogFileInfoVO>>(() => [
  { title: '#', key: 'index', width: 60, render: (_r, i) => i + 1 },
  { title: $t('page.logManagement.logFile.fileName'), key: 'fileName', ellipsis: { tooltip: true } },
  { title: $t('common.type'), key: 'type', width: 90, render: row => renderTag(row.type, 'info') },
  {
    title: $t('page.logManagement.logFile.size'),
    key: 'size',
    width: 110,
    render: row => `${(row.size / 1024).toFixed(1)} KB`
  },
  { title: $t('page.logManagement.logFile.lastModifiedAt'), key: 'lastModifiedAt', width: 180 },
  {
    title: $t('page.logManagement.logFile.compressed'),
    key: 'compressed',
    width: 80,
    render: row => renderBoolTag(row.compressed, 'warning')
  },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 180,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        hasAuth('rtd:log-file:preview') && !row.compressed
          ? h(NButton, { size: 'small', quaternary: true, onClick: () => openPreview(row) }, () =>
              $t('page.logManagement.logFile.preview')
            )
          : null,
        hasAuth('rtd:log-file:download')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => handleDownload(row) }, () =>
              $t('common.download')
            )
          : null
      ])
  }
]);

async function fetchData() {
  loading.value = true;
  try {
    const res = await getLogFileList(fileType.value);
    list.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function openPreview(row: LogFileInfoVO) {
  previewFile.value = row;
  showPreview.value = true;
  await loadTail();
}

async function loadTail() {
  if (!previewFile.value) return;
  previewLoading.value = true;
  try {
    const res = await getLogFileTail(fileType.value, previewFile.value.fileName, previewLines.value);
    previewContent.value = res.data?.content ?? [];
  } finally {
    previewLoading.value = false;
  }
}

async function handleDownload(row: LogFileInfoVO) {
  const { data: blob, error } = await downloadLogFile(fileType.value, row.fileName);
  if (error || !blob) return;
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = row.fileName;
  a.click();
  URL.revokeObjectURL(url);
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.logManagement.logFile.title') }}</span>
          <NSpace>
            <NRadioGroup v-model:value="fileType" @update:value="fetchData">
              <NRadioButton v-for="t in TYPES" :key="t.value" :value="t.value">{{ t.label }}</NRadioButton>
            </NRadioGroup>
            <NButton @click="fetchData">{{ $t('common.refresh') }}</NButton>
          </NSpace>
        </div>
      </template>
      <DataTable flex-height :columns="columns" :data="list" :loading="loading" :bordered="false" size="small" />
    </NCard>

    <NModal
      v-model:show="showPreview"
      preset="card"
      :title="$t('page.logManagement.logFile.previewTitle', { name: previewFile?.fileName || '' })"
      :style="{ width: '90vw', maxWidth: '1100px' }"
    >
      <NSpace class="mb-12px" align="center">
        <span>{{ $t('page.logManagement.logFile.lines') }}</span>
        <NSelect
          v-model:value="previewLines"
          style="width: 120px"
          :options="[
            { label: '200', value: 200 },
            { label: '500', value: 500 },
            { label: '1000', value: 1000 },
            { label: '5000', value: 5000 },
            { label: 'ALL', value: 'ALL' }
          ]"
          @update:value="loadTail"
        />
        <PermissionGuard code="rtd:log-file:preview">
          <NButton size="small" :loading="previewLoading" @click="loadTail">
            {{ $t('page.logManagement.logFile.refreshPreview') }}
          </NButton>
        </PermissionGuard>
      </NSpace>
      <LargeViewer :lines="previewContent" :loading="previewLoading" :height="420" />
    </NModal>
  </div>
</template>
