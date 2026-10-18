<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { getProductList, type Product } from '@/service/api/mes/product';
import { getRecipeList, type Recipe } from '@/service/api/mes/recipe';
import { getStepList, type Step } from '@/service/api/mes/step';
import {
  createStepRecipe,
  deleteStepRecipe,
  getOwnedStepRecipes,
  updateStepRecipe,
  type StepRecipe
} from '@/service/api/mes/step-recipe';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesStepRecipe' });

const { hasAuth } = useAuth();
const steps = ref<Step[]>([]);
const recipes = ref<Recipe[]>([]);
const products = ref<Product[]>([]);
const stepId = ref<string | null>(null);
const loading = ref(false);
const rows = ref<StepRecipe[]>([]);
const showModal = ref(false);
const editing = ref<StepRecipe | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({ recipeId: null as string | null, productId: null as string | null, isDefault: false });

const rules = computed<FormRules>(() => ({
  recipeId: [{ required: true, message: $t('page.mes.stepRecipe.required'), trigger: 'change' }]
}));
const stepOptions = computed(() => steps.value.map(item => ({ label: `${item.stepCode} ${item.stepName}`, value: String(item.id) })));
const recipeOptions = computed(() => recipes.value.map(item => ({ label: `${item.recipeCode} ${item.recipeName ?? ''}`, value: String(item.id) })));
const productOptions = computed(() => products.value.map(item => ({ label: `${item.productCode} ${item.productName ?? ''}`, value: String(item.id) })));

function recipeLabel(id?: string | null) {
  const recipe = recipes.value.find(item => String(item.id) === String(id));
  return recipe ? `${recipe.recipeCode} ${recipe.recipeName ?? ''}`.trim() : String(id ?? '');
}

function productLabel(id?: string | null) {
  if (!id || id === '0') return $t('page.mes.stepRecipe.generic');
  const product = products.value.find(item => String(item.id) === String(id));
  return product ? `${product.productCode} ${product.productName ?? ''}`.trim() : String(id);
}

function openCreate() {
  editing.value = null;
  Object.assign(form, { recipeId: null, productId: null, isDefault: false });
  showModal.value = true;
}

function openEdit(row: StepRecipe) {
  editing.value = row;
  Object.assign(form, {
    recipeId: String(row.recipeId),
    productId: !row.productId || row.productId === '0' ? null : String(row.productId),
    isDefault: row.isDefault === 1
  });
  showModal.value = true;
}

const columns = computed<DataTableColumns<StepRecipe>>(() => [
  { title: $t('page.mes.stepRecipe.recipe'), key: 'recipeId', render: row => recipeLabel(row.recipeId) },
  { title: $t('page.mes.stepRecipe.product'), key: 'productId', render: row => productLabel(row.productId) },
  {
    title: $t('page.mes.stepRecipe.defaultRecipe'),
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
        hasAuth('mes:step-recipe:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:step-recipe:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);

async function fetchData() {
  if (!stepId.value) {
    rows.value = [];
    return;
  }
  loading.value = true;
  try {
    const res = await getOwnedStepRecipes(stepId.value);
    rows.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

async function submit() {
  await formRef.value?.validate();
  if (!stepId.value || !form.recipeId) return;
  const data = {
    stepId: stepId.value,
    recipeId: form.recipeId,
    productId: form.productId,
    isDefault: form.isDefault ? 1 : 0
  };
  if (editing.value) await updateStepRecipe(String(editing.value.id), data);
  else await createStepRecipe(data);
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
}

function handleDelete(row: StepRecipe) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.stepRecipe.deleteConfirm'),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteStepRecipe(String(row.id));
      window.$message?.success($t('common.saveSuccess'));
      await fetchData();
    }
  });
}

onMounted(async () => {
  const [stepPage, recipePage, productPage] = await Promise.all([
    getStepList({ page: 1, pageSize: 200 }),
    getRecipeList({ page: 1, pageSize: 200 }),
    getProductList({ page: 1, pageSize: 200 })
  ]);
  steps.value = stepPage.data?.records ?? [];
  recipes.value = recipePage.data?.records ?? [];
  products.value = productPage.data?.records ?? [];
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.stepRecipe.title') }}</span>
          <div class="flex items-center gap-8px">
            <NSelect v-model:value="stepId" class="w-260px" :options="stepOptions" filterable @update:value="fetchData" />
            <PermissionGuard code="mes:step-recipe:add">
              <NButton type="primary" :disabled="!stepId" @click="openCreate">{{ $t('common.add') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.stepRecipe.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog
      v-model:show="showModal"
      :title="editing ? $t('page.mes.stepRecipe.editTitle') : $t('page.mes.stepRecipe.addTitle')"
      class="w-560px"
      :on-confirm="submit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="110">
        <NFormItem :label="$t('page.mes.stepRecipe.recipe')" path="recipeId">
          <NSelect v-model:value="form.recipeId" :options="recipeOptions" filterable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.stepRecipe.product')" path="productId">
          <NSelect v-model:value="form.productId" :options="productOptions" filterable clearable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.stepRecipe.defaultRecipe')" path="isDefault">
          <NSwitch v-model:value="form.isDefault" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
