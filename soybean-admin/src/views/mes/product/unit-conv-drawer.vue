<script setup lang="ts">
import { computed, h, reactive, ref, watch } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules, type SelectOption } from 'naive-ui';
import type { CountUnit } from '@/service/api/mes/count-unit';
import {
  checkProductUnitConv,
  createProductUnitConv,
  deleteProductUnitConv,
  getProductUnitConvList,
  updateProductUnitConv,
  type ProductUnitConv
} from '@/service/api/mes/product-unit-conv';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

const props = defineProps<{
  show: boolean;
  productId: string;
  productCode: string;
  units: CountUnit[];
}>();

const emit = defineEmits<{ 'update:show': [value: boolean] }>();

const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<ProductUnitConv[]>([]);
const editing = ref<ProductUnitConv | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  fromUnitId: null as string | null,
  toUnitId: null as string | null,
  factor: null as number | null,
  tolerancePct: 5 as number | null,
  remark: ''
});
const checkForm = reactive({
  fromQty: 1 as number | null,
  actualQty: null as number | null
});

const unitOptions = computed<SelectOption[]>(() => props.units.map(item => ({ label: item.unitCode, value: item.id })));
const visible = computed({
  get: () => props.show,
  set: value => emit('update:show', value)
});

const rules = computed<FormRules>(() => ({
  fromUnitId: [{ required: true, message: $t('page.mes.productUnitConv.required'), trigger: 'change' }],
  toUnitId: [{ required: true, message: $t('page.mes.productUnitConv.required'), trigger: 'change' }],
  factor: [{ required: true, type: 'number', message: $t('page.mes.productUnitConv.required'), trigger: 'blur' }]
}));

function unitLabel(id: string) {
  return props.units.find(item => item.id === id)?.unitCode ?? id;
}

function resetForm() {
  editing.value = null;
  Object.assign(form, { fromUnitId: null, toUnitId: null, factor: null, tolerancePct: 5, remark: '' });
}

async function load() {
  if (!props.productId) return;
  loading.value = true;
  try {
    const { data } = await getProductUnitConvList(props.productId, { page: 1, pageSize: 50 });
    rows.value = data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

watch(
  () => [props.show, props.productId] as const,
  ([show]) => {
    if (show) {
      resetForm();
      load();
    }
  }
);

function editRow(row: ProductUnitConv) {
  editing.value = row;
  Object.assign(form, {
    fromUnitId: row.fromUnitId,
    toUnitId: row.toUnitId,
    factor: row.factor,
    tolerancePct: row.tolerancePct,
    remark: row.remark ?? ''
  });
}

async function save() {
  await formRef.value?.validate();
  const payload = {
    fromUnitId: form.fromUnitId!,
    toUnitId: form.toUnitId!,
    factor: form.factor!,
    tolerancePct: form.tolerancePct,
    remark: form.remark
  };
  if (editing.value) {
    await updateProductUnitConv(props.productId, editing.value.id, payload);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createProductUnitConv(props.productId, payload);
    window.$message?.success($t('common.addSuccess'));
  }
  resetForm();
  await load();
}

async function checkRow(row: ProductUnitConv) {
  if (checkForm.fromQty == null || checkForm.actualQty == null) {
    window.$message?.warning($t('page.mes.productUnitConv.required'));
    return;
  }
  const { data, error } = await checkProductUnitConv(props.productId, {
    fromUnitId: row.fromUnitId,
    toUnitId: row.toUnitId,
    fromQty: checkForm.fromQty,
    actualQty: checkForm.actualQty
  });
  if (!error && data) {
    const flag = data.withinTolerance ? $t('page.mes.productUnitConv.inside') : $t('page.mes.productUnitConv.outside');
    window.$message?.success(`${flag} ${data.referenceQty} / ${data.deviationPct}%`);
  }
}

function remove(row: ProductUnitConv) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.productUnitConv.deleteConfirm', { name: `${unitLabel(row.fromUnitId)} → ${unitLabel(row.toUnitId)}` }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteProductUnitConv(props.productId, row.id);
      window.$message?.success($t('common.deleteSuccess'));
      await load();
    }
  });
}

const columns = computed<DataTableColumns<ProductUnitConv>>(() => [
  { title: $t('page.mes.productUnitConv.fromUnit'), key: 'fromUnitId', render: row => unitLabel(row.fromUnitId) },
  { title: $t('page.mes.productUnitConv.toUnit'), key: 'toUnitId', render: row => unitLabel(row.toUnitId) },
  { title: $t('page.mes.productUnitConv.factor'), key: 'factor', width: 100 },
  { title: $t('page.mes.productUnitConv.tolerance'), key: 'tolerancePct', width: 90 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 220,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        h(NButton, { size: 'small', quaternary: true, onClick: () => checkRow(row) }, () => $t('page.mes.productUnitConv.check')),
        hasAuth('mes:product:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => editRow(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:product:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => remove(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);
</script>

<template>
  <NModal v-model:show="visible" preset="card" class="w-860px" :title="`${$t('page.mes.productUnitConv.title')} ${productCode}`">
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
    <DataTable :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96" class="mt-16px">
      <div class="grid grid-cols-2 gap-x-12px">
        <NFormItem :label="$t('page.mes.productUnitConv.fromUnit')" path="fromUnitId">
          <NSelect v-model:value="form.fromUnitId" :options="unitOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.productUnitConv.toUnit')" path="toUnitId">
          <NSelect v-model:value="form.toUnitId" :options="unitOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.productUnitConv.factor')" path="factor">
          <NInputNumber v-model:value="form.factor" :min="0" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.productUnitConv.tolerance')" path="tolerancePct">
          <NInputNumber v-model:value="form.tolerancePct" :min="0" class="w-full" />
        </NFormItem>
      </div>
      <div class="flex justify-end gap-8px">
        <NButton @click="resetForm">{{ $t('common.reset') }}</NButton>
        <NButton v-if="hasAuth('mes:product:add') || (editing && hasAuth('mes:product:edit'))" type="primary" @click="save">
          {{ editing ? $t('common.save') : $t('common.add') }}
        </NButton>
      </div>
    </NForm>
  </NModal>
</template>
