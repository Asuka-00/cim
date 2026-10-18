<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getProductList, type Product } from '@/service/api/mes/product';
import { getRecipeList, type Recipe } from '@/service/api/mes/recipe';
import { getRouteList, type Route } from '@/service/api/mes/route';
import { getRouteStepList, type RouteStep } from '@/service/api/mes/route-step';
import { getRouteVersionList, type RouteVersion } from '@/service/api/mes/route-version';
import {
  createRouteStepRecipe,
  deleteRouteStepRecipe,
  getOwnedRouteStepRecipes,
  updateRouteStepRecipe,
  type RouteStepRecipe
} from '@/service/api/mes/route-step-recipe';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesRouteStepRecipe' });

const { hasAuth } = useAuth();
const routes = ref<Route[]>([]);
const versions = ref<RouteVersion[]>([]);
const steps = ref<RouteStep[]>([]);
const recipes = ref<Recipe[]>([]);
const products = ref<Product[]>([]);
const routeId = ref<string | null>(null);
const versionId = ref<string | null>(null);
const routeStepId = ref<string | null>(null);
const loading = ref(false);
const rows = ref<RouteStepRecipe[]>([]);
const showModal = ref(false);
const editing = ref<RouteStepRecipe | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({ recipeId: null as string | null, productId: null as string | null, isDefault: false });

const formRules = computed<FormRules>(() => ({
  recipeId: [{ required: true, message: $t('page.mes.routeStepRecipe.required'), trigger: 'change' }]
}));
const routeOptions = computed(() => routes.value.map(item => ({ label: `${item.routeCode} ${item.routeName}`, value: String(item.id) })));
const versionOptions = computed(() => versions.value.map(item => ({ label: `${item.versionNo} ${item.status}`, value: String(item.id) })));
const stepOptions = computed(() => steps.value.map(item => ({ label: `${item.seqNo} ${item.stepAlias ?? ''}`.trim(), value: String(item.id) })));
const recipeOptions = computed(() => recipes.value.map(item => ({ label: `${item.recipeCode} ${item.recipeName}`, value: String(item.id) })));
const productOptions = computed(() => products.value.map(item => ({ label: `${item.productCode} ${item.productName}`, value: String(item.id) })));
const draft = computed(() => versions.value.find(item => String(item.id) === versionId.value)?.status === 'DRAFT');

function recipeLabel(id?: string | null) {
  const recipe = recipes.value.find(item => String(item.id) === String(id));
  return recipe ? `${recipe.recipeCode} ${recipe.recipeName}` : String(id ?? '');
}

function productLabel(id?: string | null) {
  if (!id || id === '0') return $t('page.mes.routeStepRecipe.generic');
  const product = products.value.find(item => String(item.id) === String(id));
  return product ? `${product.productCode} ${product.productName}` : String(id);
}

function openCreate() {
  editing.value = null;
  Object.assign(form, { recipeId: null, productId: null, isDefault: false });
  showModal.value = true;
}

function openEdit(row: RouteStepRecipe) {
  editing.value = row;
  Object.assign(form, {
    recipeId: String(row.recipeId),
    productId: !row.productId || row.productId === '0' ? null : String(row.productId),
    isDefault: row.isDefault === 1
  });
  showModal.value = true;
}

const columns = computed<DataTableColumns<RouteStepRecipe>>(() => [
  { title: $t('page.mes.routeStepRecipe.recipe'), key: 'recipeId', render: row => recipeLabel(row.recipeId) },
  { title: $t('page.mes.routeStepRecipe.product'), key: 'productId', render: row => productLabel(row.productId) },
  {
    title: $t('page.mes.routeStepRecipe.defaultRecipe'),
    key: 'isDefault',
    width: 90,
    render: row => (row.isDefault === 1 ? $t('common.yesOrNo.yes') : $t('common.yesOrNo.no'))
  },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 140,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        draft.value && hasAuth('mes:route-step-recipe:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        draft.value && hasAuth('mes:route-step-recipe:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);

async function onRouteChange(value: string | null) {
  versionId.value = null;
  routeStepId.value = null;
  versions.value = [];
  steps.value = [];
  rows.value = [];
  if (!value) return;
  const page = await getRouteVersionList(value, { page: 1, pageSize: 100 });
  versions.value = page.data?.records ?? [];
}

async function onVersionChange(value: string | null) {
  routeStepId.value = null;
  steps.value = [];
  rows.value = [];
  if (!value) return;
  const res = await getRouteStepList(value);
  steps.value = res.data ?? [];
}

async function fetchData() {
  if (!routeStepId.value) {
    rows.value = [];
    return;
  }
  loading.value = true;
  try {
    const res = await getOwnedRouteStepRecipes(routeStepId.value);
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function submit() {
  await formRef.value?.validate();
  if (!routeStepId.value || !form.recipeId || !draft.value) return;
  const data = {
    routeStepId: routeStepId.value,
    recipeId: form.recipeId,
    productId: form.productId,
    isDefault: form.isDefault ? 1 : 0
  };
  if (editing.value) await updateRouteStepRecipe(String(editing.value.id), data);
  else await createRouteStepRecipe(data);
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

function handleDelete(row: RouteStepRecipe) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.routeStepRecipe.deleteConfirm'),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteRouteStepRecipe(String(row.id));
      window.$message?.success($t('common.saveSuccess'));
      await fetchData();
    }
  });
}

onMounted(async () => {
  const [routePage, recipePage, productPage] = await Promise.all([
    getRouteList({ page: 1, pageSize: 200 }),
    getRecipeList({ page: 1, pageSize: 200 }),
    getProductList({ page: 1, pageSize: 200 })
  ]);
  routes.value = routePage.data?.records ?? [];
  recipes.value = recipePage.data?.records ?? [];
  products.value = productPage.data?.records ?? [];
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.routeStepRecipe.title') }}</span>
          <div class="flex items-center gap-8px">
            <NSelect v-model:value="routeId" class="w-220px" :options="routeOptions" filterable @update:value="onRouteChange" />
            <NSelect v-model:value="versionId" class="w-160px" :options="versionOptions" :disabled="!routeId" @update:value="onVersionChange" />
            <NSelect v-model:value="routeStepId" class="w-180px" :options="stepOptions" :disabled="!versionId" filterable @update:value="fetchData" />
            <PermissionGuard code="mes:route-step-recipe:add">
              <NButton type="primary" :disabled="!routeStepId || !draft" @click="openCreate">{{ $t('common.add') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.routeStepRecipe.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog
      v-model:show="showModal"
      :title="editing ? $t('page.mes.routeStepRecipe.editTitle') : $t('page.mes.routeStepRecipe.addTitle')"
      class="w-560px"
      :on-confirm="submit"
    >
      <NForm ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="110">
        <NFormItem :label="$t('page.mes.routeStepRecipe.recipe')" path="recipeId">
          <NSelect v-model:value="form.recipeId" :options="recipeOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeStepRecipe.product')" path="productId">
          <NSelect v-model:value="form.productId" :options="productOptions" filterable clearable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.routeStepRecipe.defaultRecipe')" path="isDefault">
          <NSwitch v-model:value="form.isDefault" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
