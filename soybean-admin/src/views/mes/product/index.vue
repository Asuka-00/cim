<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules, type SelectOption } from 'naive-ui';
import { getCountUnitList, type CountUnit } from '@/service/api/mes/count-unit';
import { getCustomerList, type Customer } from '@/service/api/mes/customer';
import { getProductFamilyList, type ProductFamily } from '@/service/api/mes/product-family';
import { createProduct, deleteProduct, getProductList, updateProduct, type Product } from '@/service/api/mes/product';
import UnitConvDrawer from './unit-conv-drawer.vue';
import VersionDrawer from './version-drawer.vue';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesProduct' });

const { hasAuth } = useAuth();
const units = ref<CountUnit[]>([]);
const families = ref<ProductFamily[]>([]);
const customers = ref<Customer[]>([]);

const unitOptions = computed<SelectOption[]>(() => units.value.map(item => ({ label: item.unitCode, value: item.id })));
const familyOptions = computed<SelectOption[]>(() =>
  families.value.map(item => ({ label: item.familyCode, value: item.id }))
);
const customerOptions = computed<SelectOption[]>(() =>
  customers.value.map(item => ({ label: item.customerCode, value: item.id }))
);
const typeOptions = ['WAFER', 'DIE', 'PACKAGE', 'OTHER'].map(value => ({ label: value, value }));
const sizeOptions = [150, 200, 300].map(value => ({ label: String(value), value }));

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Product, { productCode: string }>({
    initialQuery: { productCode: '' },
    fetcher: getProductList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'productCode', label: $t('page.mes.product.productCode'), type: 'input' }
]);

const showModal = ref(false);
const convShow = ref(false);
const convProduct = ref<Product | null>(null);
const versionShow = ref(false);
const versionProduct = ref<Product | null>(null);
const editing = ref<Product | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  productCode: '',
  productName: '',
  productType: 'WAFER',
  familyId: null as string | null,
  customerId: null as string | null,
  customerPartNo: '',
  waferSizeMm: null as number | null,
  grossDiePerWafer: null as number | null,
  baseUnitId: null as string | null,
  remark: '',
  enabled: 1
});

const rules = computed<FormRules>(() => ({
  productCode: [{ required: true, message: $t('page.mes.product.required'), trigger: 'blur' }],
  productName: [{ required: true, message: $t('page.mes.product.required'), trigger: 'blur' }],
  baseUnitId: [{ required: true, message: $t('page.mes.product.required'), trigger: 'change' }]
}));

function blankForm() {
  Object.assign(form, {
    productCode: '',
    productName: '',
    productType: 'WAFER',
    familyId: null,
    customerId: null,
    customerPartNo: '',
    waferSizeMm: null,
    grossDiePerWafer: null,
    baseUnitId: null,
    remark: '',
    enabled: 1
  });
}

function openCreate() {
  editing.value = null;
  blankForm();
  showModal.value = true;
}

function openEdit(row: Product) {
  editing.value = row;
  Object.assign(form, {
    ...row,
    familyId: row.familyId ?? null,
    customerId: row.customerId ?? null,
    waferSizeMm: row.waferSizeMm ?? null,
    grossDiePerWafer: row.grossDiePerWafer ?? null
  });
  showModal.value = true;
}

function unitLabel(id?: string | null) {
  return units.value.find(item => item.id === id)?.unitCode ?? id ?? '';
}

const columns = computed<DataTableColumns<Product>>(() => [
  { title: $t('page.mes.product.productCode'), key: 'productCode' },
  { title: $t('page.mes.product.productName'), key: 'productName' },
  { title: $t('page.mes.product.productType'), key: 'productType', width: 110 },
  { title: $t('page.mes.product.waferSizeMm'), key: 'waferSizeMm', width: 110 },
  { title: $t('page.mes.product.baseUnit'), key: 'baseUnitId', width: 110, render: row => unitLabel(row.baseUnitId) },
  {
    title: $t('common.status'),
    key: 'enabled',
    width: 90,
    render: row => (row.enabled === 1 ? $t('common.enabled') : $t('common.disabled'))
  },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 280,
    render: row =>
      h('div', { class: 'flex gap-8px flex-wrap' }, [
        h(
          NButton,
          {
            size: 'small',
            quaternary: true,
            onClick: () => {
              versionProduct.value = row;
              versionShow.value = true;
            }
          },
          () => $t('page.mes.productVersion.title')
        ),
        h(
          NButton,
          {
            size: 'small',
            quaternary: true,
            onClick: () => {
              convProduct.value = row;
              convShow.value = true;
            }
          },
          () => $t('page.mes.product.conversions')
        ),
        hasAuth('mes:product:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:product:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  const payload = {
    ...form,
    baseUnitId: form.baseUnitId!,
    familyId: form.familyId || null,
    customerId: form.customerId || null
  };
  if (editing.value) {
    await updateProduct(String(editing.value.id), payload);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createProduct(payload);
    window.$message?.success($t('common.addSuccess'));
  }
  await fetchData();
}

function handleDelete(row: Product) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.product.deleteConfirm', { name: row.productCode }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteProduct(String(row.id));
      window.$message?.success($t('common.deleteSuccess'));
      await fetchData();
    }
  });
}

onMounted(async () => {
  const [unitRes, familyRes, customerRes] = await Promise.all([
    getCountUnitList({ page: 1, pageSize: 200 }),
    getProductFamilyList({ page: 1, pageSize: 200 }),
    getCustomerList({ page: 1, pageSize: 200 })
  ]);
  units.value = unitRes.data?.records ?? [];
  families.value = familyRes.data?.records ?? [];
  customers.value = customerRes.data?.records ?? [];
  await fetchData();
});
</script>

<template>
  <div class="rtd-page">
    <FilterBar
      :fields="filterFields"
      :model-value="searchParams as Record<string, unknown>"
      @update:model-value="v => Object.assign(searchParams, v)"
      @search="handleSearch"
      @reset="handleReset"
    />
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('page.mes.product.title') }}</span>
          <PermissionGuard code="mes:product:add">
            <NButton type="primary" @click="openCreate">{{ $t('common.add') }}</NButton>
          </PermissionGuard>
        </div>
      </template>
      <DataTable flex-height :columns="columns" :data="list" :loading="loading" :bordered="false" size="small" />
      <div class="mt-12px flex justify-end">
        <NPagination
          :page="currentPage"
          :page-size="pageSize"
          :item-count="total"
          show-size-picker
          :page-sizes="[10, 20, 50]"
          @update:page="onPageUpdate"
          @update:page-size="onSizeUpdate"
        />
      </div>
    </NCard>
    <CrudDialog
      v-model:show="showModal"
      :title="editing ? $t('page.mes.product.editTitle') : $t('page.mes.product.addTitle')"
      class="w-640px"
      :on-confirm="handleSubmit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="108">
        <NFormItem :label="$t('page.mes.product.productCode')" path="productCode">
          <NInput v-model:value="form.productCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.product.productName')" path="productName">
          <NInput v-model:value="form.productName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.product.productType')" path="productType">
          <NSelect v-model:value="form.productType" :options="typeOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.product.baseUnit')" path="baseUnitId">
          <NSelect v-model:value="form.baseUnitId" :options="unitOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.product.family')" path="familyId">
          <NSelect v-model:value="form.familyId" :options="familyOptions" filterable clearable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.product.customer')" path="customerId">
          <NSelect v-model:value="form.customerId" :options="customerOptions" filterable clearable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.product.customerPartNo')" path="customerPartNo">
          <NInput v-model:value="form.customerPartNo" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.product.waferSizeMm')" path="waferSizeMm">
          <NSelect v-model:value="form.waferSizeMm" :options="sizeOptions" clearable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.product.grossDie')" path="grossDiePerWafer">
          <NInputNumber v-model:value="form.grossDiePerWafer" :min="1" class="w-full" clearable />
        </NFormItem>
        <NFormItem :label="$t('common.remark')" path="remark">
          <NInput v-model:value="form.remark" type="textarea" :rows="2" />
        </NFormItem>
        <NFormItem :label="$t('common.status')" path="enabled">
          <NRadioGroup v-model:value="form.enabled">
            <NRadio :value="1">{{ $t('common.enabled') }}</NRadio>
            <NRadio :value="0">{{ $t('common.disabled') }}</NRadio>
          </NRadioGroup>
        </NFormItem>
      </NForm>
    </CrudDialog>
    <VersionDrawer
      v-if="versionProduct"
      v-model:show="versionShow"
      :product-id="String(versionProduct.id)"
      :product-code="versionProduct.productCode"
    />
    <UnitConvDrawer
      v-if="convProduct"
      v-model:show="convShow"
      :product-id="String(convProduct.id)"
      :product-code="convProduct.productCode"
      :units="units"
    />
  </div>
</template>
