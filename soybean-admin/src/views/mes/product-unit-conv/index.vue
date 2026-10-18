<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getCountUnitList, type CountUnit } from '@/service/api/mes/count-unit';
import { getProductList, type Product } from '@/service/api/mes/product';
import {
  checkProductUnitConv,
  createProductUnitConv,
  deleteProductUnitConv,
  getProductUnitConvList,
  updateProductUnitConv,
  type ProductUnitConv
} from '@/service/api/mes/product-unit-conv';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesProductUnitConv' });

const { hasAuth } = useAuth();
const products = ref<Product[]>([]);
const units = ref<CountUnit[]>([]);
const productId = ref<string | null>(null);
const loading = ref(false);
const rows = ref<ProductUnitConv[]>([]);
const showModal = ref(false);
const editing = ref<ProductUnitConv | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  fromUnitId: null as string | null,
  toUnitId: null as string | null,
  factor: null as number | null,
  tolerancePct: 5 as number | null,
  remark: ''
});
const checkForm = reactive({ fromQty: 1 as number | null, actualQty: null as number | null });

const rules = computed<FormRules>(() => ({
  fromUnitId: [{ required: true, message: $t('page.mes.productUnitConv.required'), trigger: 'change' }],
  toUnitId: [{ required: true, message: $t('page.mes.productUnitConv.required'), trigger: 'change' }],
  factor: [{ required: true, type: 'number', message: $t('page.mes.productUnitConv.required'), trigger: 'change' }]
}));
const productOptions = computed(() => products.value.map(item => ({ label: `${item.productCode} ${item.productName}`, value: String(item.id) })));
const unitOptions = computed(() => units.value.map(item => ({ label: `${item.unitCode} ${item.unitName}`, value: String(item.id) })));

function unitLabel(id?: string | null) {
  const unit = units.value.find(item => String(item.id) === String(id));
  return unit ? `${unit.unitCode} ${unit.unitName}` : String(id ?? '');
}

function openCreate() {
  editing.value = null;
  Object.assign(form, { fromUnitId: null, toUnitId: null, factor: null, tolerancePct: 5, remark: '' });
  showModal.value = true;
}

function openEdit(row: ProductUnitConv) {
  editing.value = row;
  Object.assign(form, {
    fromUnitId: String(row.fromUnitId),
    toUnitId: String(row.toUnitId),
    factor: row.factor,
    tolerancePct: row.tolerancePct ?? 5,
    remark: row.remark ?? ''
  });
  showModal.value = true;
}

const columns = computed<DataTableColumns<ProductUnitConv>>(() => [
  { title: $t('page.mes.productUnitConv.fromUnit'), key: 'fromUnitId', render: row => unitLabel(row.fromUnitId) },
  { title: $t('page.mes.productUnitConv.toUnit'), key: 'toUnitId', render: row => unitLabel(row.toUnitId) },
  { title: $t('page.mes.productUnitConv.factor'), key: 'factor', width: 110 },
  { title: $t('page.mes.productUnitConv.tolerance'), key: 'tolerancePct', width: 110 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 200,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        hasAuth('mes:product-unit-conv:check')
          ? h(NButton, { size: 'small', quaternary: true, onClick: () => checkRow(row) }, () => $t('page.mes.productUnitConv.check'))
          : null,
        hasAuth('mes:product-unit-conv:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:product-unit-conv:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);

async function fetchData() {
  if (!productId.value) {
    rows.value = [];
    return;
  }
  loading.value = true;
  try {
    const res = await getProductUnitConvList(productId.value, { page: 1, pageSize: 50 });
    rows.value = res.data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

async function submit() {
  await formRef.value?.validate();
  if (!productId.value || form.factor == null || form.factor <= 0) return false;
  if (form.tolerancePct != null && form.tolerancePct < 0) {
    window.$message?.warning($t('page.mes.productUnitConv.toleranceInvalid'));
    return false;
  }
  const data = {
    fromUnitId: form.fromUnitId!,
    toUnitId: form.toUnitId!,
    factor: form.factor,
    tolerancePct: form.tolerancePct,
    remark: form.remark
  };
  if (editing.value) await updateProductUnitConv(productId.value, String(editing.value.id), data);
  else await createProductUnitConv(productId.value, data);
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
  return true;
}

async function checkRow(row: ProductUnitConv) {
  if (!productId.value || checkForm.fromQty == null || checkForm.actualQty == null) {
    window.$message?.warning($t('page.mes.productUnitConv.required'));
    return;
  }
  const res = await checkProductUnitConv(productId.value, {
    fromUnitId: String(row.fromUnitId),
    toUnitId: String(row.toUnitId),
    fromQty: checkForm.fromQty,
    actualQty: checkForm.actualQty
  });
  if (res.data) {
    const flag = res.data.withinTolerance ? $t('page.mes.productUnitConv.inside') : $t('page.mes.productUnitConv.outside');
    window.$message?.success(`${flag} ${res.data.referenceQty} / ${res.data.deviationPct}%`);
  }
}

function handleDelete(row: ProductUnitConv) {
  if (!productId.value) return;
  const product = productId.value;
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.productUnitConv.deleteConfirm', { name: `${unitLabel(row.fromUnitId)} → ${unitLabel(row.toUnitId)}` }),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteProductUnitConv(product, String(row.id));
      window.$message?.success($t('common.saveSuccess'));
      await fetchData();
    }
  });
}

onMounted(async () => {
  const [productPage, unitPage] = await Promise.all([
    getProductList({ page: 1, pageSize: 200 }),
    getCountUnitList({ page: 1, pageSize: 200 })
  ]);
  products.value = productPage.data?.records ?? [];
  units.value = unitPage.data?.records ?? [];
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.productUnitConv.title') }}</span>
          <div class="flex items-center gap-8px">
            <NSelect v-model:value="productId" class="w-240px" :options="productOptions" filterable @update:value="fetchData" />
            <PermissionGuard code="mes:product-unit-conv:add">
              <NButton type="primary" :disabled="!productId" @click="openCreate">{{ $t('common.add') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.productUnitConv.hint') }}</div>
      <div class="mb-12px flex items-end gap-12px">
        <div>
          <div class="mb-4px text-12px">{{ $t('page.mes.productUnitConv.fromQty') }}</div>
          <NInputNumber v-model:value="checkForm.fromQty" :min="0" class="w-140px" />
        </div>
        <div>
          <div class="mb-4px text-12px">{{ $t('page.mes.productUnitConv.actualQty') }}</div>
          <NInputNumber v-model:value="checkForm.actualQty" :min="0" class="w-140px" />
        </div>
      </div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog
      v-model:show="showModal"
      :title="editing ? $t('page.mes.productUnitConv.editTitle') : $t('page.mes.productUnitConv.addTitle')"
      class="w-560px"
      :on-confirm="submit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="110">
        <NFormItem :label="$t('page.mes.productUnitConv.fromUnit')" path="fromUnitId">
          <NSelect v-model:value="form.fromUnitId" :options="unitOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.productUnitConv.toUnit')" path="toUnitId">
          <NSelect v-model:value="form.toUnitId" :options="unitOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.productUnitConv.factor')" path="factor">
          <NInputNumber v-model:value="form.factor" :min="0.000001" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.productUnitConv.tolerance')" path="tolerancePct">
          <NInputNumber v-model:value="form.tolerancePct" :min="0" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('common.remark')" path="remark">
          <NInput v-model:value="form.remark" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
