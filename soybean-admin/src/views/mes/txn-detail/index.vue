<script setup lang="ts">
import { ref } from 'vue';
import { getTxn, type MesTxn } from '@/service/api/mes/txn';
import { $t } from '@/locales';

defineOptions({ name: 'MesTxnDetail' });

const txnId = ref('');
const loading = ref(false);
const current = ref<MesTxn | null>(null);

async function fetchData() {
  const id = txnId.value.trim();
  if (!id) {
    window.$message?.warning($t('page.mes.txnDetail.required'));
    current.value = null;
    return;
  }
  loading.value = true;
  current.value = null;
  try {
    const res = await getTxn(id);
    if (res.error) return;
    current.value = res.data ?? null;
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
          <span>{{ $t('page.mes.txnDetail.title') }}</span>
          <div class="flex items-center gap-8px">
            <NInput v-model:value="txnId" class="w-240px" :placeholder="$t('page.mes.txnDetail.txnId')" @keyup.enter="fetchData" />
            <NButton type="primary" :loading="loading" @click="fetchData">{{ $t('common.search') }}</NButton>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.txnDetail.hint') }}</div>
      <div v-if="current" class="flex flex-col gap-8px">
        <div>{{ $t('page.mes.txn.type') }}：{{ current.txnType }}</div>
        <div>{{ $t('page.mes.txn.object') }}：{{ current.objectType }} {{ current.objectCode }}</div>
        <div>{{ $t('page.mes.txn.result') }}：{{ current.result }}</div>
        <div>{{ $t('page.mes.txn.operator') }}：{{ current.operator }}</div>
        <div>{{ $t('page.mes.txn.time') }}：{{ current.txnTime }}</div>
        <div>{{ current.errorMessage }}</div>
        <NInput :value="current.payloadJson ?? ''" type="textarea" :rows="10" readonly />
      </div>
    </NCard>
  </div>
</template>
