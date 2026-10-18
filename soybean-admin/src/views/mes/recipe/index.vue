<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import { createRecipe, deleteRecipe, getRecipeList, updateRecipe, type Recipe } from '@/service/api/mes/recipe';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';
import VersionDrawer from './version-drawer.vue';

defineOptions({ name: 'MesRecipe' });

const { hasAuth } = useAuth();
const typeOptions = ['PROCESS', 'MEASURE', 'TEST', 'PACK', 'TRANSPORT', 'OTHER'].map(value => ({ label: value, value }));

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Recipe, { recipeCode: string }>({
    initialQuery: { recipeCode: '' },
    fetcher: getRecipeList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'recipeCode', label: $t('page.mes.recipe.recipeCode'), type: 'input' }
]);

const showModal = ref(false);
const editing = ref<Recipe | null>(null);
const versionTarget = ref<Recipe | null>(null);
const versionShow = ref(false);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  recipeCode: '',
  recipeName: '',
  equipmentType: null as string | null,
  description: '',
  enabled: 1
});

const rules = computed<FormRules>(() => ({
  recipeCode: [{ required: true, message: $t('page.mes.recipe.required'), trigger: 'blur' }],
  recipeName: [{ required: true, message: $t('page.mes.recipe.required'), trigger: 'blur' }]
}));

function openCreate() {
  editing.value = null;
  Object.assign(form, { recipeCode: '', recipeName: '', equipmentType: null, description: '', enabled: 1 });
  showModal.value = true;
}

function openEdit(row: Recipe) {
  editing.value = row;
  Object.assign(form, { ...row, equipmentType: row.equipmentType || null });
  showModal.value = true;
}

const columns = computed<DataTableColumns<Recipe>>(() => [
  { title: $t('page.mes.recipe.recipeCode'), key: 'recipeCode' },
  { title: $t('page.mes.recipe.recipeName'), key: 'recipeName' },
  { title: $t('page.mes.recipe.equipmentType'), key: 'equipmentType', width: 120 },
  {
    title: $t('common.status'),
    key: 'enabled',
    width: 90,
    render: row => (row.enabled === 1 ? $t('common.enabled') : $t('common.disabled'))
  },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 220,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        h(
          NButton,
          {
            size: 'small',
            quaternary: true,
            onClick: () => {
              versionTarget.value = row;
              versionShow.value = true;
            }
          },
          () => $t('page.mes.recipe.versions')
        ),
        hasAuth('mes:recipe:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:recipe:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  const payload = { ...form, equipmentType: form.equipmentType || null };
  if (editing.value) {
    await updateRecipe(String(editing.value.id), payload);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createRecipe(payload);
    window.$message?.success($t('common.addSuccess'));
  }
  await fetchData();
}

function handleDelete(row: Recipe) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.recipe.deleteConfirm', { name: row.recipeCode }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteRecipe(String(row.id));
      window.$message?.success($t('common.deleteSuccess'));
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
          <span>{{ $t('page.mes.recipe.title') }}</span>
          <PermissionGuard code="mes:recipe:add">
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
      :title="editing ? $t('page.mes.recipe.editTitle') : $t('page.mes.recipe.addTitle')"
      class="w-560px"
      :on-confirm="handleSubmit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="108">
        <NFormItem :label="$t('page.mes.recipe.recipeCode')" path="recipeCode">
          <NInput v-model:value="form.recipeCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.recipe.recipeName')" path="recipeName">
          <NInput v-model:value="form.recipeName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.recipe.equipmentType')" path="equipmentType">
          <NSelect v-model:value="form.equipmentType" :options="typeOptions" clearable />
        </NFormItem>
        <NFormItem :label="$t('page.mes.recipe.description')" path="description">
          <NInput v-model:value="form.description" type="textarea" :rows="2" />
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
      v-if="versionTarget"
      v-model:show="versionShow"
      :recipe-id="String(versionTarget.id)"
      :recipe-code="versionTarget.recipeCode"
    />
  </div>
</template>
