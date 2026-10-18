<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst } from 'naive-ui';
import { getBomList, type Bom } from '@/service/api/mes/bom';
import { getProductList, type Product } from '@/service/api/mes/product';
import {
  approveProductVersion,
  copyProductVersion,
  createProductVersion,
  getProductVersionList,
  rejectProductVersion,
  submitProductVersion,
  updateProductVersion,
  type ProductVersion
} from '@/service/api/mes/product-version';
import { getRouteList, type Route } from '@/service/api/mes/route';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesProductVersion' });

const { hasAuth } = useAuth();
const products = ref<Product[]>([]);
const routes = ref<Route[]>([]);
const boms = ref<Bom[]>([]);
const productId = ref<string | null>(null);
const actor = ref('');
const loading = ref(false);
const rows = ref<ProductVersion[]>([]);
const showModal = ref(false);
const editing = ref<ProductVersion | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({ routeId: null as string | null, bomId: null as string | null, remark: '' });

const productOptions = computed(() => products.value.map(item => ({ label: `${item.productCode} ${item.productName}`, value: String(item.id) })));
const routeOptions = computed(() => routes.value.map(item => ({ label: `${item.routeCode} ${item.routeName}`, value: String(item.id) })));
const bomOptions = computed(() => boms.value.map(item => ({ label: `${item.bomCode} ${item.bomName}`, value: String(item.id) })));

function routeLabel(id?: string | null) {
  const route = routes.value.find(item => String(item.id) === String(id));
  return route ? `${route.routeCode} ${route.routeName}` : '';
}

function bomLabel(id?: string | null) {
  const bom = boms.value.find(item => String(item.id) === String(id));
  return bom ? `${bom.bomCode} ${bom.bomName}` : '';
}

function openCreate() {
  editing.value = null;
  Object.assign(form, { routeId: null, bomId: null, remark: '' });
  showModal.value = true;
}

function openEdit(row: ProductVersion) {
  editing.value = row;
  Object.assign(form, {
    routeId: row.routeId ? String(row.routeId) : null,
    bomId: row.bomId ? String(row.bomId) : null,
    remark: row.remark ?? ''
  });
  showModal.value = true;
}

function requireActor() {
  if (!actor.value.trim()) {
    window.$message?.warning($t('page.mes.productVersion.actorRequired'));
    return false;
  }
  return true;
}

const columns = computed<DataTableColumns<ProductVersion>>(() => [
  { title: $t('page.mes.productVersion.versionNo'), key: 'versionNo', width: 90 },
  { title: $t('page.mes.productVersion.status'), key: 'status', width: 110 },
  { title: $t('page.mes.productVersion.route'), key: 'routeId', render: row => routeLabel(row.routeId) },
  { title: $t('page.mes.productVersion.bom'), key: 'bomId', render: row => bomLabel(row.bomId) },
  { title: $t('common.remark'), key: 'remark' },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 280,
    render: row =>
      h('div', { class: 'flex gap-8px flex-wrap' }, [
        hasAuth('mes:product-version:copy')
          ? h(NButton, { size: 'small', quaternary: true, onClick: () => copy(row) }, () => $t('page.mes.productVersion.copy'))
          : null,
        row.status === 'DRAFT' && hasAuth('mes:product-version:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        row.status === 'DRAFT' && hasAuth('mes:product-version:submit')
          ? h(NButton, { size: 'small', quaternary: true, onClick: () => submit(row) }, () => $t('page.mes.productVersion.submit'))
          : null,
        row.status === 'PENDING' && hasAuth('mes:product-version:approve')
          ? h(NButton, { size: 'small', quaternary: true, type: 'success', onClick: () => approve(row) }, () => $t('page.mes.productVersion.approve'))
          : null,
        row.status === 'PENDING' && hasAuth('mes:product-version:approve')
          ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => reject(row) }, () => $t('page.mes.productVersion.reject'))
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
    const res = await getProductVersionList(productId.value, { page: 1, pageSize: 50 });
    rows.value = res.data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

async function submitForm() {
  if (!productId.value) return false;
  const data = { routeId: form.routeId, bomId: form.bomId, remark: form.remark };
  if (editing.value) await updateProductVersion(productId.value, String(editing.value.id), data);
  else await createProductVersion(productId.value, data);
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
  return true;
}

async function copy(row: ProductVersion) {
  if (!productId.value) return;
  await copyProductVersion(productId.value, String(row.id));
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
}

async function submit(row: ProductVersion) {
  if (!productId.value || !requireActor()) return;
  await submitProductVersion(productId.value, String(row.id), { assignee: actor.value.trim() });
  window.$message?.success($t('page.mes.productVersion.submitted'));
  await fetchData();
}

async function approve(row: ProductVersion) {
  if (!productId.value || !requireActor()) return;
  await approveProductVersion(productId.value, String(row.id), { operator: actor.value.trim() });
  window.$message?.success($t('page.mes.productVersion.approved'));
  await fetchData();
}

async function reject(row: ProductVersion) {
  if (!productId.value || !requireActor()) return;
  await rejectProductVersion(productId.value, String(row.id), { operator: actor.value.trim() });
  window.$message?.success($t('page.mes.productVersion.rejected'));
  await fetchData();
}

onMounted(async () => {
  const [productPage, routePage, bomPage] = await Promise.all([
    getProductList({ page: 1, pageSize: 200 }),
    getRouteList({ page: 1, pageSize: 200 }),
    getBomList({ page: 1, pageSize: 200 })
  ]);
  products.value = productPage.data?.records ?? [];
  routes.value = routePage.data?.records ?? [];
  boms.value = bomPage.data?.records ?? [];
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.productVersion.title') }}</span>
          <div class="flex items-center gap-8px">
            <NSelect v-model:value="productId" class="w-240px" :options="productOptions" filterable @update:value="fetchData" />
            <NInput v-model:value="actor" class="w-180px" :placeholder="$t('page.mes.productVersion.actorHint')" />
            <PermissionGuard code="mes:product-version:add">
              <NButton type="primary" :disabled="!productId" @click="openCreate">{{ $t('common.add') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.productVersion.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog
      v-model:show="showModal"
      :title="editing ? $t('page.mes.productVersion.editTitle') : $t('page.mes.productVersion.addTitle')"
      class="w-560px"
      :on-confirm="submitForm"
    >
      <NForm ref="formRef" :model="form" label-placement="left" label-width="110">
        <NFormItem :label="$t('page.mes.productVersion.route')" path="routeId">
          <NSelect v-model:value="form.routeId" :options="routeOptions" filterable clearable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.productVersion.bom')" path="bomId">
          <NSelect v-model:value="form.bomId" :options="bomOptions" filterable clearable />
        </NFormItem>
        <NFormItem :label="$t('common.remark')" path="remark">
          <NInput v-model:value="form.remark" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
