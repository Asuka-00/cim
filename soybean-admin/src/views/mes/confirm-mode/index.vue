<script setup lang="ts">
import { ref } from 'vue';
import { getConfirmMode } from '@/service/api/mes/confirm-rule';
import { $t } from '@/locales';

defineOptions({ name: 'MesConfirmMode' });

const actionCode = ref('');
const loading = ref(false);
const mode = ref('');

async function fetchData() {
  const code = actionCode.value.trim();
  if (!code) {
    window.$message?.warning($t('page.mes.confirmMode.required'));
    mode.value = '';
    return;
  }
  loading.value = true;
  mode.value = '';
  try {
    const res = await getConfirmMode(code);
    if (res.error) return;
    mode.value = res.data ?? '';
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.confirmMode.title') }}</span>
          <div class="flex items-center gap-8px">
            <NInput v-model:value="actionCode" class="w-240px" :placeholder="$t('page.mes.confirmMode.action')" @keyup.enter="fetchData" />
            <NButton type="primary" :loading="loading" @click="fetchData">{{ $t('common.search') }}</NButton>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.confirmMode.hint') }}</div>
      <div v-if="mode">{{ $t('page.mes.confirmMode.mode') }}：{{ mode }}</div>
    </NCard>
  </div>
</template>
