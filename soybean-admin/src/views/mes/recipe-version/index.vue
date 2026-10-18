<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst } from 'naive-ui';
import { getRecipeList, type Recipe } from '@/service/api/mes/recipe';
import {
  approveRecipeVersion,
  copyRecipeVersion,
  createRecipeVersion,
  deleteRecipeVersion,
  getRecipeVersionList,
  rejectRecipeVersion,
  submitRecipeVersion,
  updateRecipeVersion,
  type RecipeVersion
} from '@/service/api/mes/recipe-version';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesRecipeVersion' });

const { hasAuth } = useAuth();
const recipes = ref<Recipe[]>([]);
const recipeId = ref<string | null>(null);
const actor = ref('');
const loading = ref(false);
const rows = ref<RecipeVersion[]>([]);
const showModal = ref(false);
const editing = ref<RecipeVersion | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({ paramDesc: '', remark: '' });

const recipeOptions = computed(() => recipes.value.map(item => ({ label: `${item.recipeCode} ${item.recipeName}`, value: String(item.id) })));

function openCreate() {
  editing.value = null;
  Object.assign(form, { paramDesc: '', remark: '' });
  showModal.value = true;
}

function openEdit(row: RecipeVersion) {
  editing.value = row;
  Object.assign(form, { paramDesc: row.paramDesc ?? '', remark: row.remark ?? '' });
  showModal.value = true;
}

function requireActor() {
  if (!actor.value.trim()) {
    window.$message?.warning($t('page.mes.recipeVersion.actorRequired'));
    return false;
  }
  return true;
}

const columns = computed<DataTableColumns<RecipeVersion>>(() => [
  { title: $t('page.mes.recipeVersion.versionNo'), key: 'versionNo', width: 90 },
  { title: $t('page.mes.recipeVersion.status'), key: 'status', width: 110 },
  { title: $t('page.mes.recipeVersion.paramDesc'), key: 'paramDesc' },
  { title: $t('common.remark'), key: 'remark' },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 320,
    render: row =>
      h('div', { class: 'flex gap-8px flex-wrap' }, [
        hasAuth('mes:recipe-version:copy')
          ? h(NButton, { size: 'small', quaternary: true, onClick: () => copy(row) }, () => $t('page.mes.recipeVersion.copy'))
          : null,
        row.status === 'DRAFT' && hasAuth('mes:recipe-version:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        row.status === 'DRAFT' && hasAuth('mes:recipe-version:submit')
          ? h(NButton, { size: 'small', quaternary: true, onClick: () => submit(row) }, () => $t('page.mes.recipeVersion.submit'))
          : null,
        row.status === 'PENDING' && hasAuth('mes:recipe-version:approve')
          ? h(NButton, { size: 'small', quaternary: true, type: 'success', onClick: () => approve(row) }, () => $t('page.mes.recipeVersion.approve'))
          : null,
        row.status === 'PENDING' && hasAuth('mes:recipe-version:approve')
          ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => reject(row) }, () => $t('page.mes.recipeVersion.reject'))
          : null,
        row.status === 'DRAFT' && hasAuth('mes:recipe-version:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);

async function fetchData() {
  if (!recipeId.value) {
    rows.value = [];
    return;
  }
  loading.value = true;
  try {
    const res = await getRecipeVersionList(recipeId.value, { page: 1, pageSize: 50 });
    rows.value = res.data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

async function submitForm() {
  if (!recipeId.value) return false;
  const data = { paramDesc: form.paramDesc, remark: form.remark };
  if (editing.value) await updateRecipeVersion(recipeId.value, String(editing.value.id), data);
  else await createRecipeVersion(recipeId.value, data);
  window.$message?.success($t('common.saveSuccess'));
  await fetchData();
  return true;
}

async function copy(row: RecipeVersion) {
  if (!recipeId.value) return;
  await copyRecipeVersion(recipeId.value, String(row.id));
  window.$message?.success($t('common.addSuccess'));
  await fetchData();
}

async function submit(row: RecipeVersion) {
  if (!recipeId.value || !requireActor()) return;
  await submitRecipeVersion(recipeId.value, String(row.id), { assignee: actor.value.trim() });
  window.$message?.success($t('page.mes.recipeVersion.submitted'));
  await fetchData();
}

async function approve(row: RecipeVersion) {
  if (!recipeId.value || !requireActor()) return;
  await approveRecipeVersion(recipeId.value, String(row.id), { operator: actor.value.trim() });
  window.$message?.success($t('page.mes.recipeVersion.activated'));
  await fetchData();
}

async function reject(row: RecipeVersion) {
  if (!recipeId.value || !requireActor()) return;
  await rejectRecipeVersion(recipeId.value, String(row.id), { operator: actor.value.trim() });
  window.$message?.success($t('page.mes.recipeVersion.rejected'));
  await fetchData();
}

function handleDelete(row: RecipeVersion) {
  if (!recipeId.value) return;
  const recipe = recipeId.value;
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.recipeVersion.deleteConfirm', { name: String(row.versionNo) }),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteRecipeVersion(recipe, String(row.id));
      window.$message?.success($t('common.saveSuccess'));
      await fetchData();
    }
  });
}

onMounted(async () => {
  const page = await getRecipeList({ page: 1, pageSize: 200 });
  recipes.value = page.data?.records ?? [];
});
</script>

<template>
  <div class="rtd-page">
    <NCard :bordered="false" class="rtd-page-card rounded-8px shadow-sm">
      <template #header>
        <div class="flex items-center justify-between gap-12px">
          <span>{{ $t('page.mes.recipeVersion.title') }}</span>
          <div class="flex items-center gap-8px">
            <NSelect v-model:value="recipeId" class="w-240px" :options="recipeOptions" filterable @update:value="fetchData" />
            <NInput v-model:value="actor" class="w-180px" :placeholder="$t('page.mes.recipeVersion.actorHint')" />
            <PermissionGuard code="mes:recipe-version:add">
              <NButton type="primary" :disabled="!recipeId" @click="openCreate">{{ $t('common.add') }}</NButton>
            </PermissionGuard>
          </div>
        </div>
      </template>
      <div class="mb-12px">{{ $t('page.mes.recipeVersion.hint') }}</div>
      <DataTable flex-height :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    </NCard>
    <CrudDialog
      v-model:show="showModal"
      :title="editing ? $t('page.mes.recipeVersion.editTitle') : $t('page.mes.recipeVersion.addTitle')"
      class="w-560px"
      :on-confirm="submitForm"
    >
      <NForm ref="formRef" :model="form" label-placement="left" label-width="110">
        <NFormItem :label="$t('page.mes.recipeVersion.paramDesc')" path="paramDesc">
          <NInput v-model:value="form.paramDesc" type="textarea" :rows="3" />
        </NFormItem>
        <NFormItem :label="$t('common.remark')" path="remark">
          <NInput v-model:value="form.remark" />
        </NFormItem>
      </NForm>
    </CrudDialog>
  </div>
</template>
