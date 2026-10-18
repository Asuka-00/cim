<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { bindStepSampling, getStepSamplings, type StepSampling } from '@/service/api/mes/step-sampling';
import { getProductList, type Product } from '@/service/api/mes/product';
import { getSamplingRules, type SamplingRule } from '@/service/api/mes/sampling-rule';
import { getStepList, type Step } from '@/service/api/mes/step';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { $t } from '@/locales';

defineOptions({ name: 'MesStepSampling' });

const loading = ref(false);
const rows = ref<StepSampling[]>([]);
const steps = ref<Step[]>([]);
const products = ref<Product[]>([]);
const rules = ref<SamplingRule[]>([]);
const showCreate = ref(false);
const formRef = ref<FormInst | null>(null);
const form = reactive({ stepId: null as string | null, productId: null as string | null, ruleId: null as string | null });

const rulesForm = computed<FormRules>(() => ({
  stepId: [{ required: true, message: $t('page.mes.stepSampling.required'), trigger: 'change' }],
  ruleId: [{ required: true, message: $t('page.mes.stepSampling.required'), trigger: 'change' }]
}));

const stepOptions = computed(() => steps.value.map(item => ({ label: `${item.stepCode} ${item.stepName}`, value: String(item.id) })));
const productOptions = computed(() => products.value.map(item => ({ label: `${item.productCode} ${item.productName}`, value: String(item.id) })));
const ruleOptions = computed(() =>
  rules.value.filter(item => item.enabled !== 0).map(item => ({ label: `${item.ruleCode} ${item.ruleName}`, value: String(item.id) }))
);

const stepName = computed(() => {
  const names = new Map(steps.value.map(item => [String(item.id), `${item.stepCode} ${item.stepName}`]));
  return (id: string) => names.get(String(id)) ?? id;
});
const productName = computed(() => {
  const names = new Map(products.value.map(item => [String(item.id), `${item.productCode} ${item.productName}`]));
  return (id?: string | number) => (!id || String(id) === '0' ? $t('page.mes.stepSampling.anyProduct') : (names.get(String(id)) ?? String(id)));
});
const ruleName = computed(() => {
  const names = new Map(rules.value.map(item => [String(item.id), `${item.ruleCode} ${item.ruleName}`]));
  return (id: string) => names.get(String(id)) ?? id;
});

function openCreate() {
  Object.assign(form, { stepId: null, productId: null, ruleId: null });
  showCreate.value = true;
}

const columns = computed<DataTableColumns<StepSampling>>(() => [
  { title: $t('page.mes.stepSampling.step'), key: 'stepId', render: row => stepName.value(row.stepId) },
  { title: $t('page.mes.stepSampling.product'), key: 'productId', render: row => productName.value(row.productId) },
  { title: $t('page.mes.stepSampling.rule'), key: 'ruleId', render: row => ruleName.value(row.ruleId) }
]);

async function fetchData() {
  loading.value = true;
  try {
    const [bindRes, stepPage, productPage, ruleRes] = await Promise.all([
      getStepSamplings(),
      getStepList({ page: 1, pageSize: 200 }),
      getProductList({ page: 1, pageSize: 200 }),
      getSamplingRules()
    ]);
    rows.value = bindRes.data ?? [];
    steps.value = stepPage.data?.records ?? [];
    products.value = productPage.data?.records ?? [];
    rules.value = ruleRes.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function submitCreate() {
  await formRef.value?.validate();
  if (!form.stepId || !form.ruleId) return;
  await bindStepSampling({ stepId: form.stepId, productId: form.productId, ruleId: form.ruleId });
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
}

onMounted(fetchData);
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.stepSampling.title') }}</span>
          <PermissionGuard code="mes:step-sampling:add">
            <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
          </PermissionGuard>
        </div>
      </template>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showCreate" :title="$t('page.mes.stepSampling.addTitle')" class="w-640px" :on-confirm="submitCreate">
      <div class="mb-12px">{{ $t('page.mes.stepSampling.hint') }}</div>
      <NForm ref="formRef" :model="form" :rules="rulesForm" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.stepSampling.step')" path="stepId">
          <NSelect v-model:value="form.stepId" :options="stepOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.stepSampling.product')" path="productId">
          <NSelect v-model:value="form.productId" :options="productOptions" clearable filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.stepSampling.rule')" path="ruleId">
          <NSelect v-model:value="form.ruleId" :options="ruleOptions" filterable />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
