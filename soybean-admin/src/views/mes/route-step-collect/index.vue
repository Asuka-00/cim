<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getRouteStepCollectItems, saveRouteStepCollectItem, type RouteStepCollectItem } from '@/service/api/mes/route-step-collect';
import { getHoldReasonList, type HoldReason } from '@/service/api/mes/hold';
import { getProductList, type Product } from '@/service/api/mes/product';
import { getRouteList, type Route } from '@/service/api/mes/route';
import { getRouteStepList, type RouteStep } from '@/service/api/mes/route-step';
import { getRouteVersionList, type RouteVersion } from '@/service/api/mes/route-version';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { $t } from '@/locales';

defineOptions({ name: 'MesRouteStepCollect' });

const loading = ref(false);
const rows = ref<RouteStepCollectItem[]>([]);
const routes = ref<Route[]>([]);
const versions = ref<RouteVersion[]>([]);
const steps = ref<RouteStep[]>([]);
const products = ref<Product[]>([]);
const reasons = ref<HoldReason[]>([]);
const routeId = ref<string | null>(null);
const versionId = ref<string | null>(null);
const showCreate = ref(false);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  seqNo: null as number | null,
  itemId: '',
  productId: null as string | null,
  lsl: null as number | null,
  usl: null as number | null,
  autoHold: false,
  holdReasonCode: null as string | null,
  required: true
});

const formRules = computed<FormRules>(() => ({
  seqNo: [{ required: true, type: 'number', message: $t('page.mes.routeCollect.required'), trigger: 'change' }],
  itemId: [{ required: true, message: $t('page.mes.routeCollect.required'), trigger: 'blur' }],
  holdReasonCode: form.autoHold ? [{ required: true, message: $t('page.mes.routeCollect.required'), trigger: 'change' }] : []
}));

const routeOptions = computed(() => routes.value.map(item => ({ label: `${item.routeCode} ${item.routeName}`, value: String(item.id) })));
const versionOptions = computed(() => versions.value.map(item => ({ label: `${item.versionNo} ${item.status}`, value: String(item.id) })));
const seqOptions = computed(() => steps.value.map(item => ({ label: `${item.seqNo} ${item.stepAlias ?? ''}`.trim(), value: item.seqNo })));
const productOptions = computed(() => products.value.map(item => ({ label: `${item.productCode} ${item.productName}`, value: String(item.id) })));
const reasonOptions = computed(() =>
  reasons.value.filter(item => item.enabled === 1).map(item => ({ label: `${item.reasonCode} ${item.reasonName}`, value: item.reasonCode }))
);
const seqName = computed(() => {
  const names = new Map(steps.value.map(item => [String(item.id), String(item.seqNo)]));
  return (id: string) => names.get(String(id)) ?? id;
});
const productName = computed(() => {
  const names = new Map(products.value.map(item => [String(item.id), `${item.productCode} ${item.productName}`]));
  return (id?: string | number) => (!id || String(id) === '0' ? $t('page.mes.routeCollect.anyProduct') : (names.get(String(id)) ?? String(id)));
});

const columns = computed<DataTableColumns<RouteStepCollectItem>>(() => [
  { title: $t('page.mes.routeCollect.seq'), key: 'routeStepId', width: 90, render: row => seqName.value(row.routeStepId) },
  { title: $t('page.mes.routeCollect.item'), key: 'itemId', width: 140 },
  { title: $t('page.mes.routeCollect.product'), key: 'productId', render: row => productName.value(row.productId) },
  { title: $t('page.mes.routeCollect.lsl'), key: 'lsl', width: 80 },
  { title: $t('page.mes.routeCollect.usl'), key: 'usl', width: 80 },
  { title: $t('page.mes.routeCollect.reason'), key: 'holdReasonCode', width: 120 }
]);

async function onRouteChange(value: string | null) {
  versionId.value = null;
  versions.value = [];
  steps.value = [];
  rows.value = [];
  if (!value) return;
  const page = await getRouteVersionList(value, { page: 1, pageSize: 100 });
  versions.value = page.data?.records ?? [];
}

async function onVersionChange(value: string | null) {
  steps.value = [];
  rows.value = [];
  if (!value) return;
  loading.value = true;
  try {
    const [stepRes, itemRes] = await Promise.all([getRouteStepList(value), getRouteStepCollectItems(value)]);
    steps.value = stepRes.data ?? [];
    rows.value = itemRes.data ?? [];
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  Object.assign(form, { seqNo: null, itemId: '', productId: null, lsl: null, usl: null, autoHold: false, holdReasonCode: null, required: true });
  showCreate.value = true;
}

async function submitCreate() {
  await formRef.value?.validate();
  if (!versionId.value || form.seqNo == null) return false;
  if (form.lsl == null && form.usl == null) {
    window.$message?.warning($t('page.mes.routeCollect.limitHint'));
    return false;
  }
  await saveRouteStepCollectItem({
    routeVersionId: versionId.value,
    seqNo: form.seqNo,
    itemId: form.itemId,
    productId: form.productId,
    lsl: form.lsl,
    usl: form.usl,
    autoHoldOnOos: form.autoHold ? 1 : 0,
    holdReasonCode: form.autoHold ? form.holdReasonCode ?? undefined : undefined,
    isRequired: form.required ? 1 : 0
  });
  window.$message?.success($t('common.saveSuccess'));
  await onVersionChange(versionId.value);
}

onMounted(async () => {
  const [routePage, productPage, reasonPage] = await Promise.all([
    getRouteList({ page: 1, pageSize: 200 }),
    getProductList({ page: 1, pageSize: 200 }),
    getHoldReasonList({ page: 1, pageSize: 200 })
  ]);
  routes.value = routePage.data?.records ?? [];
  products.value = productPage.data?.records ?? [];
  reasons.value = reasonPage.data?.records ?? [];
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.routeCollect.title') }}</span>
          <div class="flex items-center gap-8px">
            <NSelect v-model:value="routeId" class="w-220px" :options="routeOptions" filterable @update:value="onRouteChange" />
            <NSelect v-model:value="versionId" class="w-160px" :options="versionOptions" @update:value="onVersionChange" />
            <PermissionGuard code="mes:route-collect:add">
              <NButton type="primary" :disabled="!versionId" @click="openCreate">{{ $t('common.add') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.routeCollect.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showCreate" :title="$t('page.mes.routeCollect.addTitle')" class="w-640px" :on-confirm="submitCreate">
      <NForm ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="110">
        <NFormItem :label="$t('page.mes.routeCollect.seq')" path="seqNo">
          <NSelect v-model:value="form.seqNo" :options="seqOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeCollect.item')" path="itemId">
          <NInput v-model:value="form.itemId" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeCollect.product')" path="productId">
          <NSelect v-model:value="form.productId" :options="productOptions" clearable filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeCollect.lsl')" path="lsl">
          <NInputNumber v-model:value="form.lsl" clearable class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeCollect.usl')" path="usl">
          <NInputNumber v-model:value="form.usl" clearable class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeCollect.requiredFlag')" path="required">
          <NSwitch v-model:value="form.required" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeCollect.autoHold')" path="autoHold">
          <NSwitch v-model:value="form.autoHold" />
        </NFormItem>
        <NFormItem v-if="form.autoHold" :label="$t('page.mes.routeCollect.reason')" path="holdReasonCode">
          <NSelect v-model:value="form.holdReasonCode" :options="reasonOptions" filterable />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
