<script setup lang="ts">
import { ref } from 'vue';
import { scanTimeLink } from '@/service/api/mes/time-link-scan';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { $t } from '@/locales';

defineOptions({ name: 'MesTimeLinkScan' });

const now = ref<string | null>(null);
const count = ref<number | null>(null);
const loading = ref(false);

async function scan() {
  loading.value = true;
  try {
    const res = await scanTimeLink(now.value || undefined);
    count.value = res.data ?? 0;
    window.$message?.success($t('page.mes.timeLink.done', { count: count.value }));
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <span>{{ $t('page.mes.timeLink.title') }}</span>
      </template>
      <div class="mb-12px">{{ $t('page.mes.timeLink.hint') }}</div>
      <div class="flex items-center gap-8px">
        <NDatePicker v-model:formatted-value="now" type="datetime" value-format="yyyy-MM-dd'T'HH:mm:ss" clearable />
        <PermissionGuard code="mes:time-link:scan">
          <NButton type="primary" :loading="loading" @click="scan">{{ $t('page.mes.timeLink.scan') }}</NButton>
        </PermissionGuard>
      </div>
      <div v-if="count != null" class="mt-16px">{{ $t('page.mes.timeLink.result', { count }) }}</div>
    </NCard>
  </div>
</template>
