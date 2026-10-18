<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import type { DataTableColumns } from 'naive-ui';
import { expireMaterialLots } from '@/service/api/mes/expire';
import { getMaterialLots, type MaterialLot } from '@/service/api/mes/inspection';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { $t } from '@/locales';

defineOptions({ name: 'MesExpire' });

const loading = ref(false);
const rows = ref<MaterialLot[]>([]);
const today = ref<string | null>(null);

const columns = computed<DataTableColumns<MaterialLot>>(() => [
  { title: $t('page.mes.expire.lotCode'), key: 'lotCode' },
  { title: $t('page.mes.expire.status'), key: 'status', width: 130 },
  { title: $t('page.mes.expire.expireDate'), key: 'expireDate', width: 140 }
]);

async function fetchData() {
  loading.value = true;
  try {
    const res = await getMaterialLots();
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function handleExpire() {
  const res = await expireMaterialLots(today.value ?? undefined);
  window.$message?.success($t('page.mes.expire.done', { count: res.data ?? 0 }));
  await fetchData();
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.expire.title') }}</span>
          <div class="flex items-center gap-8px">
            <NDatePicker v-model:formatted-value="today" type="date" value-format="yyyy-MM-dd" clearable />
            <PermissionGuard code="mes:material-lot:expire">
              <NButton type="warning" @click="handleExpire">{{ $t('page.mes.expire.run') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
  </div>
</template>
