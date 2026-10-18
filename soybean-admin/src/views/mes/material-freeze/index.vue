<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue';
import { NButton, type DataTableColumns } from 'naive-ui';
import { getMaterialLots, type MaterialLot } from '@/service/api/mes/inspection';
import { freezeMaterialLot, unfreezeMaterialLot } from '@/service/api/mes/material-freeze';
import { getMaterialList, type Material } from '@/service/api/mes/material';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesMaterialFreeze' });

const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<MaterialLot[]>([]);
const materials = ref<Material[]>([]);

const materialName = computed(() => {
  const names = new Map(materials.value.map(item => [String(item.id), `${item.materialCode} ${item.materialName}`]));
  return (id: string) => names.get(String(id)) ?? id;
});

async function fetchData() {
  loading.value = true;
  try {
    const [lotRes, materialPage] = await Promise.all([getMaterialLots(), getMaterialList({ page: 1, pageSize: 200 })]);
    rows.value = lotRes.data ?? [];
    materials.value = materialPage.data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

function run(row: MaterialLot, freeze: boolean) {
  window.$dialog?.warning({
    title: freeze ? $t('page.mes.materialFreeze.freeze') : $t('page.mes.materialFreeze.unfreeze'),
    content: $t(freeze ? 'page.mes.materialFreeze.freezeConfirm' : 'page.mes.materialFreeze.unfreezeConfirm', {
      name: row.lotCode
    }),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      if (freeze) await freezeMaterialLot(String(row.id));
      else await unfreezeMaterialLot(String(row.id));
      window.$message?.success($t('common.saveSuccess'));
      await fetchData();
    }
  });
}

const columns = computed<DataTableColumns<MaterialLot>>(() => [
  { title: $t('page.mes.materialFreeze.lotCode'), key: 'lotCode' },
  { title: $t('page.mes.materialFreeze.material'), key: 'materialId', render: row => materialName.value(row.materialId) },
  { title: $t('page.mes.materialFreeze.status'), key: 'status', width: 130 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 120,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        row.status === 'AVAILABLE' && hasAuth('mes:material-lot:freeze')
          ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => run(row, true) }, () =>
              $t('page.mes.materialFreeze.freeze')
            )
          : null,
        row.status === 'FROZEN' && hasAuth('mes:material-lot:unfreeze')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => run(row, false) }, () =>
              $t('page.mes.materialFreeze.unfreeze')
            )
          : null
      ])
  }
]);

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <span>{{ $t('page.mes.materialFreeze.title') }}</span>
      </template>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
  </div>
</template>
