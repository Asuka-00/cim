<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getStepCollectItems, saveStepCollectItem, type StepCollectItem } from '@/service/api/mes/step-collect';
import { getHoldReasonList, type HoldReason } from '@/service/api/mes/hold';
import { getProductList, type Product } from '@/service/api/mes/product';
import { getStepList, type Step } from '@/service/api/mes/step';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { $t } from '@/locales';

defineOptions({ name: 'MesStepCollect' });

const steps = ref<Step[]>([]);
const products = ref<Product[]>([]);
const reasons = ref<HoldReason[]>([]);
const stepId = ref<string | null>(null);
const loading = ref(false);
const rows = ref<StepCollectItem[]>([]);
const showCreate = ref(false);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  itemId: '',
  productId: null as string | null,
  lsl: null as number | null,
  usl: null as number | null,
  autoHold: false,
  holdReasonCode: null as string | null,
  required: true
});

const rules = computed<FormRules>(() => ({
  itemId: [{ required: true, message: $t('page.mes.collectSpec.required'), trigger: 'blur' }],
  holdReasonCode: form.autoHold ? [{ required: true, message: $t('page.mes.collectSpec.required'), trigger: 'change' }] : []
}));
const stepOptions = computed(() => steps.value.map(item => ({ label: `${item.stepCode} ${item.stepName}`, value: String(item.id) })));
const productOptions = computed(() => products.value.map(item => ({ label: `${item.productCode} ${item.productName}`, value: String(item.id) })));
const reasonOptions = computed(() =>
  reasons.value.filter(item => item.enabled === 1).map(item => ({ label: `${item.reasonCode} ${item.reasonName}`, value: item.reasonCode }))
);
const productName = computed(() => {
  const names = new Map(products.value.map(item => [String(item.id), `${item.productCode} ${item.productName}`]));
  return (id?: string | number) => (!id || String(id) === '0' ? $t('page.mes.collectSpec.anyProduct') : (names.get(String(id)) ?? String(id)));
});

const columns = computed<DataTableColumns<StepCollectItem>>(() => [
  { title: $t('page.mes.collectSpec.item'), key: 'itemId', width: 140 },
  { title: $t('page.mes.collectSpec.product'), key: 'productId', render: row => productName.value(row.productId) },
  { title: $t('page.mes.collectSpec.lsl'), key: 'lsl', width: 90 },
  { title: $t('page.mes.collectSpec.usl'), key: 'usl', width: 90 },
  { title: $t('page.mes.collectSpec.requiredFlag'), key: 'isRequired', width: 80, render: row => (row.isRequired === 0 ? '' : $t('page.mes.collectSpec.yes')) },
  { title: $t('page.mes.collectSpec.autoHold'), key: 'holdReasonCode', width: 140 }
]);

function openCreate() {
  Object.assign(form, { itemId: '', productId: null, lsl: null, usl: null, autoHold: false, holdReasonCode: null, required: true });
  showCreate.value = true;
}

async function fetchData() {
  if (!stepId.value) {
    rows.value = [];
    return;
  }
  loading.value = true;
  try {
    const res = await getStepCollectItems(stepId.value);
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function submitCreate() {
  await formRef.value?.validate();
  if (!stepId.value) return;
  if (form.lsl == null && form.usl == null) {
    window.$message?.warning($t('page.mes.collectSpec.limitHint'));
    return false;
  }
  await saveStepCollectItem({
    stepId: stepId.value,
    itemId: form.itemId,
    productId: form.productId,
    lsl: form.lsl,
    usl: form.usl,
    autoHoldOnOos: form.autoHold ? 1 : 0,
    holdReasonCode: form.autoHold ? form.holdReasonCode ?? undefined : undefined,
    isRequired: form.required ? 1 : 0
  });
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

onMounted(async () => {
  const [stepPage, productPage, reasonPage] = await Promise.all([
    getStepList({ page: 1, pageSize: 200 }),
    getProductList({ page: 1, pageSize: 200 }),
    getHoldReasonList({ page: 1, pageSize: 200 })
  ]);
  steps.value = stepPage.data?.records ?? [];
  products.value = productPage.data?.records ?? [];
  reasons.value = reasonPage.data?.records ?? [];
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.collectSpec.title') }}</span>
          <div class="flex items-center gap-8px">
            <NSelect v-model:value="stepId" class="w-240px" :options="stepOptions" filterable :placeholder="$t('page.mes.collectSpec.step')" />
            <NButton @click="fetchData">{{ $t('common.search') }}</NButton>
            <PermissionGuard code="mes:collect-spec:add">
              <NButton type="primary" :disabled="!stepId" @click="openCreate">{{ $t('common.add') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.collectSpec.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog v-model:show="showCreate" :title="$t('page.mes.collectSpec.addTitle')" class="w-640px" :on-confirm="submitCreate">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="110">
        <NFormItem :label="$t('page.mes.collectSpec.item')" path="itemId">
          <NInput v-model:value="form.itemId" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.collectSpec.product')" path="productId">
          <NSelect v-model:value="form.productId" :options="productOptions" clearable filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.collectSpec.lsl')" path="lsl">
          <NInputNumber v-model:value="form.lsl" clearable class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.collectSpec.usl')" path="usl">
          <NInputNumber v-model:value="form.usl" clearable class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.collectSpec.requiredFlag')" path="required">
          <NSwitch v-model:value="form.required" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.collectSpec.autoHold')" path="autoHold">
          <NSwitch v-model:value="form.autoHold" />
        </NFormItem>
        <NFormItem v-if="form.autoHold" :label="$t('page.mes.collectSpec.reason')" path="holdReasonCode">
          <NSelect v-model:value="form.holdReasonCode" :options="reasonOptions" filterable />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
