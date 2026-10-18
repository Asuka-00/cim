<script setup lang="ts">
import { ref } from 'vue';
import { initMesTenant } from '@/service/api/mes/tenant';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { localStg } from '@/utils/storage';
import { $t } from '@/locales';

defineOptions({ name: 'MesTenantInit' });

const tenantId = ref(localStg.get('loginTenantId') || '');
const loading = ref(false);

function submit() {
  const id = tenantId.value.trim();
  if (!id) {
    window.$message?.warning($t('page.mes.tenantInit.required'));
    return;
  }
  window.$dialog?.warning({
    title: $t('page.mes.tenantInit.title'),
    content: $t('page.mes.tenantInit.confirm', { name: id }),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      loading.value = true;
      try {
        await initMesTenant(id);
        window.$message?.success($t('page.mes.tenantInit.done'));
      } finally {
        loading.value = false;
      }
    }
  });
}
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>{{ $t('page.mes.tenantInit.title') }}</template>
      <div class="mb-12px">{{ $t('page.mes.tenantInit.hint') }}</div>
      <div class="flex items-center gap-8px">
        <NInput v-model:value="tenantId" class="w-280px" :placeholder="$t('page.mes.tenantInit.tenantId')" />
        <PermissionGuard code="mes:tenant:init">
          <NButton type="primary" :loading="loading" @click="submit">{{ $t('page.mes.tenantInit.run') }}</NButton>
        </PermissionGuard>
      </div>
    </NCard>
  </div>
</template>
