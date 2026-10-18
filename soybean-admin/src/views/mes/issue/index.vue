<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getInventories, type InventoryRow } from '@/service/api/mes/inventory';
import { issueMaterial, returnMaterial } from '@/service/api/mes/issue';
import { getLocations, type StorageLocation } from '@/service/api/mes/location';
import { getMaterialList, type Material } from '@/service/api/mes/material';
import CrudDialog from '@/components/business/crud-dialog.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesIssue' });

const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<InventoryRow[]>([]);
const materials = ref<Material[]>([]);
const locations = ref<StorageLocation[]>([]);
const showModal = ref(false);
const issuing = ref(true);
const formRef = ref<FormInst | null>(null);
const current = ref<InventoryRow | null>(null);
const form = reactive({ toLocationId: null as string | null, qty: null as number | null });

const rules = computed<FormRules>(() => ({
  toLocationId: [{ required: true, message: $t('page.mes.issue.required'), trigger: 'change' }],
  qty: [{ required: true, type: 'number', message: $t('page.mes.issue.required'), trigger: 'blur' }]
}));

const materialName = computed(() => {
  const names = new Map(materials.value.map(item => [String(item.id), `${item.materialCode} ${item.materialName}`]));
  return (id: string) => names.get(String(id)) ?? id;
});
const locationName = computed(() => {
  const names = new Map(locations.value.map(item => [String(item.id), item.locCode]));
  return (id: string) => names.get(String(id)) ?? id;
});
const targetOptions = computed(() =>
  locations.value
    .filter(item => !current.value || String(item.id) !== String(current.value.locationId))
    .map(item => ({ label: item.locCode, value: String(item.id) }))
);

function open(row: InventoryRow, issue: boolean) {
  current.value = row;
  issuing.value = issue;
  form.toLocationId = null;
  form.qty = null;
  showModal.value = true;
}

const columns = computed<DataTableColumns<InventoryRow>>(() => [
  { title: $t('page.mes.issue.material'), key: 'materialId', render: row => materialName.value(row.materialId) },
  { title: $t('page.mes.issue.location'), key: 'locationId', render: row => locationName.value(row.locationId) },
  { title: $t('page.mes.issue.qtyOnHand'), key: 'qtyOnHand', width: 120 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 160,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        hasAuth('mes:inventory:issue')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => open(row, true) }, () =>
              $t('page.mes.issue.issue')
            )
          : null,
        hasAuth('mes:inventory:return')
          ? h(NButton, { size: 'small', quaternary: true, onClick: () => open(row, false) }, () => $t('page.mes.issue.return'))
          : null
      ])
  }
]);

async function fetchData() {
  loading.value = true;
  try {
    const [inventoryRes, materialPage, locationRes] = await Promise.all([
      getInventories(),
      getMaterialList({ page: 1, pageSize: 200 }),
      getLocations()
    ]);
    rows.value = inventoryRes.data ?? [];
    materials.value = materialPage.data?.records ?? [];
    locations.value = locationRes.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function handleSubmit() {
  await formRef.value?.validate();
  if (!current.value || !form.toLocationId || form.qty == null) return;
  const payload = {
    materialLotId: String(current.value.materialLotId),
    fromLocationId: String(current.value.locationId),
    toLocationId: form.toLocationId,
    qty: form.qty
  };
  if (issuing.value) await issueMaterial(payload);
  else await returnMaterial(payload);
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <span>{{ $t('page.mes.issue.title') }}</span>
      </template>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog
      v-model:show="showModal"
      :title="issuing ? $t('page.mes.issue.issueTitle') : $t('page.mes.issue.returnTitle')"
      class="w-560px"
      :on-confirm="handleSubmit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.issue.toLocation')" path="toLocationId">
          <NSelect v-model:value="form.toLocationId" :options="targetOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.issue.qty')" path="qty">
          <NInputNumber v-model:value="form.qty" :min="0" class="w-full" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
