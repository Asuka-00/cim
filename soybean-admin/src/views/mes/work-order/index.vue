<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getCountUnitList, type CountUnit } from '@/service/api/mes/count-unit';
import { getProductList, type Product } from '@/service/api/mes/product';
import { getProductVersionList, type ProductVersion } from '@/service/api/mes/product-version';
import {
  cancelWorkOrder,
  createWorkOrder,
  getWorkOrderList,
  releaseWorkOrder,
  type WorkOrder
} from '@/service/api/mes/work-order';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesWorkOrder' });

const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<WorkOrder, { woNo: string }>({
    initialQuery: { woNo: '' },
    fetcher: getWorkOrderList
  });

const filterFields = computed<FilterField[]>(() => [{ key: 'woNo', label: $t('page.mes.workOrder.woNo'), type: 'input' }]);

const showModal = ref(false);
const formRef = ref<FormInst | null>(null);
const products = ref<Product[]>([]);
const versions = ref<ProductVersion[]>([]);
const units = ref<CountUnit[]>([]);
const form = reactive({
  woNo: '',
  productId: null as string | null,
  productVersionId: null as string | null,
  planQty: null as number | null,
  qtyUnitId: null as string | null,
  priority: 5,
  dueDate: null as string | null,
  remark: ''
});

const rules = computed<FormRules>(() => ({
  woNo: [{ required: true, message: $t('page.mes.workOrder.required'), trigger: 'blur' }],
  productVersionId: [{ required: true, message: $t('page.mes.workOrder.required'), trigger: 'change' }],
  planQty: [{ required: true, type: 'number', message: $t('page.mes.workOrder.required'), trigger: 'blur' }],
  qtyUnitId: [{ required: true, message: $t('page.mes.workOrder.required'), trigger: 'change' }]
}));

const productOptions = computed(() => products.value.map(item => ({ label: `${item.productCode} ${item.productName}`, value: String(item.id) })));
const versionOptions = computed(() => versions.value.map(item => ({ label: `${item.versionNo} ${item.status}`, value: String(item.id) })));
const unitOptions = computed(() => units.value.map(item => ({ label: `${item.unitCode} ${item.unitName}`, value: String(item.id) })));

async function openCreate() {
  Object.assign(form, {
    woNo: '',
    productId: null,
    productVersionId: null,
    planQty: null,
    qtyUnitId: null,
    priority: 5,
    dueDate: null,
    remark: ''
  });
  versions.value = [];
  const [productPage, unitPage] = await Promise.all([
    getProductList({ page: 1, pageSize: 200 }),
    getCountUnitList({ page: 1, pageSize: 200 })
  ]);
  products.value = productPage.data?.records ?? [];
  units.value = unitPage.data?.records ?? [];
  showModal.value = true;
}

async function onProductChange(productId: string | null) {
  form.productId = productId;
  form.productVersionId = null;
  versions.value = [];
  if (!productId) return;
  const page = await getProductVersionList(productId, { page: 1, pageSize: 100 });
  versions.value = page.data?.records ?? [];
}

const columns = computed<DataTableColumns<WorkOrder>>(() => [
  { title: $t('page.mes.workOrder.woNo'), key: 'woNo' },
  { title: $t('page.mes.workOrder.status'), key: 'status', width: 120 },
  { title: $t('page.mes.workOrder.planQty'), key: 'planQty', width: 100 },
  { title: $t('page.mes.workOrder.priority'), key: 'priority', width: 90 },
  { title: $t('page.mes.workOrder.dueDate'), key: 'dueDate', width: 120 },
  { title: $t('page.mes.workOrder.lotCount'), key: 'lotCount', width: 90 },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 160,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        row.status === 'CREATED' && hasAuth('mes:work-order:release')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => handleRelease(row) }, () =>
              $t('page.mes.workOrder.release')
            )
          : null,
        row.status === 'CREATED' && hasAuth('mes:work-order:cancel')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleCancel(row) }, () =>
              $t('page.mes.workOrder.cancel')
            )
          : null
      ])
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  await createWorkOrder({
    woNo: form.woNo,
    productVersionId: form.productVersionId!,
    planQty: form.planQty!,
    qtyUnitId: form.qtyUnitId!,
    priority: form.priority,
    dueDate: form.dueDate,
    remark: form.remark
  });
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
}

function handleRelease(row: WorkOrder) {
  window.$dialog?.warning({
    title: $t('page.mes.workOrder.release'),
    content: $t('page.mes.workOrder.releaseConfirm', { name: row.woNo }),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      const res = await releaseWorkOrder(String(row.id));
      const warnings = res.data ?? [];
      if (warnings.length) window.$message?.warning(warnings.join('；'));
      else window.$message?.success($t('common.saveSuccess'));
      await fetchData();
    }
  });
}

function handleCancel(row: WorkOrder) {
  window.$dialog?.warning({
    title: $t('page.mes.workOrder.cancel'),
    content: $t('page.mes.workOrder.cancelConfirm', { name: row.woNo }),
    positiveText: $t('page.mes.workOrder.cancel'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await cancelWorkOrder(String(row.id));
      window.$message?.success($t('common.saveSuccess'));
      await fetchData();
    }
  });
}

onMounted(fetchData);
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
          <span>{{ $t('page.mes.workOrder.title') }}</span>
          <PermissionGuard code="mes:work-order:add">
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
    <CrudDialog v-model:show="showModal" :title="$t('page.mes.workOrder.addTitle')" class="w-560px" :on-confirm="handleSubmit">
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.workOrder.woNo')" path="woNo">
          <NInput v-model:value="form.woNo" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.workOrder.product')" path="productId">
          <NSelect v-model:value="form.productId" :options="productOptions" filterable @update:value="onProductChange" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.workOrder.productVersion')" path="productVersionId">
          <NSelect v-model:value="form.productVersionId" :options="versionOptions" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.workOrder.planQty')" path="planQty">
          <NInputNumber v-model:value="form.planQty" :min="1" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.workOrder.qtyUnit')" path="qtyUnitId">
          <NSelect v-model:value="form.qtyUnitId" :options="unitOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.workOrder.priority')" path="priority">
          <NInputNumber v-model:value="form.priority" :min="1" :max="9" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.workOrder.dueDate')" path="dueDate">
          <NDatePicker v-model:formatted-value="form.dueDate" type="date" value-format="yyyy-MM-dd" clearable class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('common.remark')" path="remark">
          <NInput v-model:value="form.remark" type="textarea" :rows="2" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
