<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getRouteStepSamplings, saveRouteStepSampling, type RouteStepSampling } from '@/service/api/mes/route-step-sampling';
import { getProductList, type Product } from '@/service/api/mes/product';
import { getRouteList, type Route } from '@/service/api/mes/route';
import { getRouteStepList, type RouteStep } from '@/service/api/mes/route-step';
import { getRouteVersionList, type RouteVersion } from '@/service/api/mes/route-version';
import { getSamplingRules, type SamplingRule } from '@/service/api/mes/sampling-rule';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { $t } from '@/locales';

defineOptions({ name: 'MesRouteStepSampling' });

const loading = ref(false);
const rows = ref<RouteStepSampling[]>([]);
const routes = ref<Route[]>([]);
const versions = ref<RouteVersion[]>([]);
const steps = ref<RouteStep[]>([]);
const products = ref<Product[]>([]);
const rules = ref<SamplingRule[]>([]);
const routeId = ref<string | null>(null);
const versionId = ref<string | null>(null);
const showCreate = ref(false);
const formRef = ref<FormInst | null>(null);
const form = reactive({ seqNo: null as number | null, productId: null as string | null, ruleId: null as string | null });

const formRules = computed<FormRules>(() => ({
  seqNo: [{ required: true, type: 'number', message: $t('page.mes.routeSampling.required'), trigger: 'change' }],
  ruleId: [{ required: true, message: $t('page.mes.routeSampling.required'), trigger: 'change' }]
}));

const routeOptions = computed(() => routes.value.map(item => ({ label: `${item.routeCode} ${item.routeName}`, value: String(item.id) })));
const versionOptions = computed(() => versions.value.map(item => ({ label: `${item.versionNo} ${item.status}`, value: String(item.id) })));
const seqOptions = computed(() => steps.value.map(item => ({ label: `${item.seqNo} ${item.stepAlias ?? ''}`.trim(), value: item.seqNo })));
const productOptions = computed(() => products.value.map(item => ({ label: `${item.productCode} ${item.productName}`, value: String(item.id) })));
const ruleOptions = computed(() =>
  rules.value.filter(item => item.enabled !== 0).map(item => ({ label: `${item.ruleCode} ${item.ruleName}`, value: String(item.id) }))
);

const seqName = computed(() => {
  const names = new Map(steps.value.map(item => [String(item.id), String(item.seqNo)]));
  return (id: string) => names.get(String(id)) ?? id;
});
const productName = computed(() => {
  const names = new Map(products.value.map(item => [String(item.id), `${item.productCode} ${item.productName}`]));
  return (id?: string | number) => (!id || String(id) === '0' ? $t('page.mes.routeSampling.anyProduct') : (names.get(String(id)) ?? String(id)));
});
const ruleName = computed(() => {
  const names = new Map(rules.value.map(item => [String(item.id), `${item.ruleCode} ${item.ruleName}`]));
  return (id: string) => names.get(String(id)) ?? id;
});

const columns = computed<DataTableColumns<RouteStepSampling>>(() => [
  { title: $t('page.mes.routeSampling.seq'), key: 'routeStepId', render: row => seqName.value(row.routeStepId) },
  { title: $t('page.mes.routeSampling.product'), key: 'productId', render: row => productName.value(row.productId) },
  { title: $t('page.mes.routeSampling.rule'), key: 'ruleId', render: row => ruleName.value(row.ruleId) }
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
    const [stepRes, samplingRes] = await Promise.all([getRouteStepList(value), getRouteStepSamplings(value)]);
    steps.value = stepRes.data ?? [];
    rows.value = samplingRes.data ?? [];
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  Object.assign(form, { seqNo: null, productId: null, ruleId: null });
  showCreate.value = true;
}

async function submitCreate() {
  await formRef.value?.validate();
  if (!versionId.value || form.seqNo == null || !form.ruleId) return;
  await saveRouteStepSampling({
    routeVersionId: versionId.value,
    seqNo: form.seqNo,
    productId: form.productId,
    ruleId: form.ruleId
  });
  window.$message?.success($t('common.addSuccess'));
  await onVersionChange(versionId.value);
}

onMounted(async () => {
  const [routePage, productPage, ruleRes] = await Promise.all([
    getRouteList({ page: 1, pageSize: 200 }),
    getProductList({ page: 1, pageSize: 200 }),
    getSamplingRules()
  ]);
  routes.value = routePage.data?.records ?? [];
  products.value = productPage.data?.records ?? [];
  rules.value = ruleRes.data ?? [];
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.routeSampling.title') }}</span>
          <div class="flex items-center gap-8px">
            <NSelect v-model:value="routeId" class="w-220px" :options="routeOptions" filterable @update:value="onRouteChange" />
            <NSelect v-model:value="versionId" class="w-180px" :options="versionOptions" @update:value="onVersionChange" />
            <PermissionGuard code="mes:route-sampling:add">
              <NButton type="primary" :disabled="!versionId" @click="openCreate">{{ $t('common.add') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.routeSampling.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showCreate" :title="$t('page.mes.routeSampling.addTitle')" class="w-640px" :on-confirm="submitCreate">
      <NForm ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.routeSampling.seq')" path="seqNo">
          <NSelect v-model:value="form.seqNo" :options="seqOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeSampling.product')" path="productId">
          <NSelect v-model:value="form.productId" :options="productOptions" clearable filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeSampling.rule')" path="ruleId">
          <NSelect v-model:value="form.ruleId" :options="ruleOptions" filterable />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
