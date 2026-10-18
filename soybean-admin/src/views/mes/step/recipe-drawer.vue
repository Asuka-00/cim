<script setup lang="ts">
import { computed, h, reactive, ref, watch } from 'vue';
import { NButton, type DataTableColumns, type SelectOption } from 'naive-ui';
import { getProductList, type Product } from '@/service/api/mes/product';
import { getRecipeList, type Recipe } from '@/service/api/mes/recipe';
import {
  createStepRecipe,
  deleteStepRecipe,
  getOwnedStepRecipes,
  updateStepRecipe,
  type StepRecipe
} from '@/service/api/mes/step-recipe';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

const props = defineProps<{
  show: boolean;
  stepId: string;
  stepCode: string;
}>();

const emit = defineEmits<{ 'update:show': [value: boolean] }>();

const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<StepRecipe[]>([]);
const recipes = ref<Recipe[]>([]);
const products = ref<Product[]>([]);
const editing = ref<StepRecipe | null>(null);
const form = reactive({ recipeId: null as string | null, productId: null as string | null, isDefault: 0 });
const recipeOptions = computed<SelectOption[]>(() => recipes.value.map(item => ({ label: item.recipeCode, value: item.id })));
const productOptions = computed<SelectOption[]>(() => products.value.map(item => ({ label: item.productCode, value: item.id })));

const visible = computed({
  get: () => props.show,
  set: value => emit('update:show', value)
});

function labelOf(options: SelectOption[], id?: string | null) {
  return String(options.find(item => item.value === id)?.label ?? '');
}

function resetForm() {
  editing.value = null;
  form.recipeId = null;
  form.productId = null;
  form.isDefault = 0;
}

async function load() {
  loading.value = true;
  try {
    const [bindings, recipePage, productPage] = await Promise.all([
      getOwnedStepRecipes(props.stepId),
      getRecipeList({ page: 1, pageSize: 200 }),
      getProductList({ page: 1, pageSize: 200 })
    ]);
    rows.value = bindings.data ?? [];
    recipes.value = recipePage.data?.records ?? [];
    products.value = productPage.data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

watch(
  () => [props.show, props.stepId] as const,
  ([show]) => {
    if (show) {
      resetForm();
      load();
    }
  }
);

function editRow(row: StepRecipe) {
  editing.value = row;
  form.recipeId = row.recipeId;
  form.productId = row.productId === '0' ? null : row.productId;
  form.isDefault = row.isDefault;
}

async function save() {
  if (!form.recipeId) {
    window.$message?.warning($t('page.mes.stepRecipe.required'));
    return;
  }
  const payload = { stepId: props.stepId, recipeId: form.recipeId, productId: form.productId || null, isDefault: form.isDefault };
  if (editing.value) {
    await updateStepRecipe(editing.value.id, payload);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createStepRecipe(payload);
    window.$message?.success($t('common.addSuccess'));
  }
  resetForm();
  await load();
}

function remove(row: StepRecipe) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.stepRecipe.deleteConfirm'),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteStepRecipe(row.id);
      window.$message?.success($t('common.deleteSuccess'));
      await load();
    }
  });
}

const columns = computed<DataTableColumns<StepRecipe>>(() => [
  { title: $t('page.mes.stepRecipe.recipe'), key: 'recipeId', render: row => labelOf(recipeOptions.value, row.recipeId) },
  {
    title: $t('page.mes.stepRecipe.product'),
    key: 'productId',
    render: row => (row.productId === '0' ? $t('page.mes.stepRecipe.generic') : labelOf(productOptions.value, row.productId))
  },
  {
    title: $t('page.mes.stepRecipe.defaultRecipe'),
    key: 'isDefault',
    width: 80,
    render: row => (row.isDefault === 1 ? $t('common.yesOrNo.yes') : $t('common.yesOrNo.no'))
  },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 160,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        hasAuth('mes:step:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => editRow(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:step:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => remove(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);
</script>

<template>
  <NModal v-model:show="visible" preset="card" class="w-760px" :title="`${$t('page.mes.stepRecipe.title')} ${stepCode}`">
    <DataTable :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    <NForm :model="form" label-placement="left" label-width="96" class="mt-16px">
      <NFormItem :label="$t('page.mes.stepRecipe.recipe')">
        <NSelect v-model:value="form.recipeId" :options="recipeOptions" filterable />
      </NFormItem>
      <NFormItem :label="$t('page.mes.stepRecipe.product')">
        <NSelect v-model:value="form.productId" :options="productOptions" filterable clearable />
      </NFormItem>
      <NFormItem :label="$t('page.mes.stepRecipe.defaultRecipe')">
        <NRadioGroup v-model:value="form.isDefault">
          <NRadio :value="1">{{ $t('common.yesOrNo.yes') }}</NRadio>
          <NRadio :value="0">{{ $t('common.yesOrNo.no') }}</NRadio>
        </NRadioGroup>
      </NFormItem>
      <div class="flex justify-end gap-8px">
        <NButton @click="resetForm">{{ $t('common.reset') }}</NButton>
        <NButton v-if="!editing && hasAuth('mes:step:add')" type="primary" @click="save">{{ $t('common.add') }}</NButton>
        <NButton v-else-if="editing && hasAuth('mes:step:edit')" type="primary" @click="save">{{ $t('common.save') }}</NButton>
      </div>
    </NForm>
  </NModal>
</template>
