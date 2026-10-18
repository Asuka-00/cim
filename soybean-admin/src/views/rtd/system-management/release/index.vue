<script setup lang="ts">
import { computed, h, onMounted, onUnmounted, ref } from 'vue';
import { NButton, NTag, type DataTableColumns, type UploadCustomRequestOptions } from 'naive-ui';
import {
  activateRelease,
  getCurrentRelease,
  getReleaseList,
  rollbackRelease,
  uploadReleasePackage,
  type ReleaseCurrentVO,
  type ReleaseInfoVO
} from '@/service/api/rtd/system-management/release';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { formatDisplayDateTime } from '@/utils/datetime';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'RtdSystemManagementRelease' });

const { hasAuth } = useAuth();

const loading = ref(false);
const uploading = ref(false);
const list = ref<ReleaseInfoVO[]>([]);
const current = ref<ReleaseCurrentVO | null>(null);
let pollTimer: ReturnType<typeof setInterval> | null = null;

const columns = computed<DataTableColumns<ReleaseInfoVO>>(() => [
  { title: '#', key: 'index', width: 60, render: (_r, i) => i + 1 },
  { title: $t('common.version'), key: 'version', ellipsis: { tooltip: true } },
  { title: $t('page.systemManagement.release.buildTime'), key: 'buildTime', width: 180 },
  { title: 'Git Commit', key: 'gitCommit', width: 140, ellipsis: { tooltip: true } },
  {
    title: $t('page.systemManagement.release.jarSize'),
    key: 'jarSizeBytes',
    width: 110,
    render: row => (row.jarSizeBytes != null ? `${(row.jarSizeBytes / 1024 / 1024).toFixed(2)} MB` : '-')
  },
  {
    title: $t('page.systemManagement.release.flags'),
    key: 'flags',
    width: 160,
    render: row =>
      h('div', { class: 'flex gap-4px' }, [
        row.current
          ? h(NTag, { size: 'small', type: 'success' }, () => $t('page.systemManagement.release.current'))
          : null,
        row.previous
          ? h(NTag, { size: 'small', type: 'warning' }, () => $t('page.systemManagement.release.rollbackable'))
          : null
      ])
  },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 100,
    render: row =>
      !row.current && hasAuth('rtd:release:activate')
        ? h(
            NButton,
            { size: 'small', quaternary: true, type: 'primary', onClick: () => handleActivate(row.version) },
            () => $t('page.systemManagement.release.activate')
          )
        : null
  }
]);

async function fetchData() {
  loading.value = true;
  try {
    const [cur, releases] = await Promise.all([getCurrentRelease(), getReleaseList()]);
    current.value = cur.data ?? null;
    list.value = releases.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function customUpload({ file, onFinish, onError }: UploadCustomRequestOptions) {
  const raw = file.file;
  if (!raw) {
    onError();
    return;
  }
  uploading.value = true;
  try {
    await uploadReleasePackage(raw);
    window.$message?.success($t('page.systemManagement.release.uploadSuccess'));
    onFinish();
    await fetchData();
  } catch (e) {
    onError();
    throw e;
  } finally {
    uploading.value = false;
  }
}

function startPoll(expectedVersion: string) {
  if (pollTimer) clearInterval(pollTimer);
  pollTimer = setInterval(async () => {
    try {
      const res = await getCurrentRelease();
      current.value = res.data ?? null;
      if (res.data?.runningVersion === expectedVersion || res.data?.currentVersion === expectedVersion) {
        if (pollTimer) clearInterval(pollTimer);
        pollTimer = null;
        window.$message?.success($t('page.systemManagement.release.versionSwitched'));
        await fetchData();
      }
    } catch {
      /* restarting */
    }
  }, 3000);
}

async function handleActivate(version: string) {
  window.$dialog?.warning({
    title: $t('page.systemManagement.release.confirmActivateTitle'),
    content: $t('page.systemManagement.release.activateConfirm', { version }),
    positiveText: $t('page.systemManagement.release.activate'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      const res = await activateRelease(version);
      window.$message?.info(res.data?.message || $t('page.systemManagement.release.activating'));
      startPoll(version);
    }
  });
}

async function handleRollback() {
  window.$dialog?.warning({
    title: $t('page.systemManagement.release.confirmRollbackTitle'),
    content: $t('page.systemManagement.release.rollbackConfirm'),
    positiveText: $t('page.systemManagement.release.rollback'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      const res = await rollbackRelease();
      window.$message?.info(res.data?.message || $t('page.systemManagement.release.rollingBack'));
      if (res.data?.version) startPoll(res.data.version);
    }
  });
}

onMounted(fetchData);
onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer);
});
</script>

<template>
  <div class="rtd-page">
    <NCard
      :bordered="false"
      class="mb-12px rounded-8px shadow-sm"
      :title="$t('page.systemManagement.release.currentTitle')"
    >
      <NDescriptions v-if="current" label-placement="left" :column="2" size="small">
        <NDescriptionsItem :label="$t('page.systemManagement.release.currentVersion')">
          {{ current.currentVersion || '-' }}
        </NDescriptionsItem>
        <NDescriptionsItem :label="$t('page.systemManagement.release.runningVersion')">
          {{ current.runningVersion || '-' }}
        </NDescriptionsItem>
        <NDescriptionsItem :label="$t('page.systemManagement.release.buildTime')">
          {{ formatDisplayDateTime(current.buildTime) }}
        </NDescriptionsItem>
        <NDescriptionsItem label="Git Commit">{{ current.gitCommit || '-' }}</NDescriptionsItem>
        <NDescriptionsItem :label="$t('page.systemManagement.release.previousVersion')">
          {{ current.previousVersion || '-' }}
        </NDescriptionsItem>
      </NDescriptions>
      <NEmpty v-else :description="$t('page.systemManagement.release.noCurrent')" />
      <NSpace class="mt-12px">
        <PermissionGuard code="rtd:release:upload">
          <NUpload :custom-request="customUpload" :show-file-list="false" accept=".zip">
            <NButton type="primary" :loading="uploading">
              {{ $t('page.systemManagement.release.uploadPackage') }}
            </NButton>
          </NUpload>
        </PermissionGuard>
        <PermissionGuard code="rtd:release:rollback">
          <NButton :disabled="!current?.previousVersion" @click="handleRollback">
            {{ $t('page.systemManagement.release.rollback') }}
          </NButton>
        </PermissionGuard>
        <NButton @click="fetchData">{{ $t('common.refresh') }}</NButton>
      </NSpace>
    </NCard>

    <NCard :bordered="false" class="rounded-8px shadow-sm" :title="$t('page.systemManagement.release.listTitle')">
      <DataTable flex-height :columns="columns" :data="list" :loading="loading" :bordered="false" size="small" />
    </NCard>
  </div>
</template>
