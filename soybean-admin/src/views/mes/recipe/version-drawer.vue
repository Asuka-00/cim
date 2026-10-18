<script setup lang="ts">
import { computed, h, reactive, ref, watch } from 'vue';
import { NButton, type DataTableColumns, type FormInst } from 'naive-ui';
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
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

const props = defineProps<{
  show: boolean;
  recipeId: string;
  recipeCode: string;
}>();

const emit = defineEmits<{ 'update:show': [value: boolean] }>();

const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<RecipeVersion[]>([]);
const editing = ref<RecipeVersion | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({ paramDesc: '', remark: '', actor: '' });

const visible = computed({
  get: () => props.show,
  set: value => emit('update:show', value)
});

function resetForm() {
  editing.value = null;
  form.paramDesc = '';
  form.remark = '';
}

async function load() {
  if (!props.recipeId) return;
  loading.value = true;
  try {
    const { data } = await getRecipeVersionList(props.recipeId, { page: 1, pageSize: 50 });
    rows.value = data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

watch(
  () => [props.show, props.recipeId] as const,
  ([show]) => {
    if (show) {
      resetForm();
      load();
    }
  }
);

function editRow(row: RecipeVersion) {
  editing.value = row;
  form.paramDesc = row.paramDesc ?? '';
  form.remark = row.remark ?? '';
}

async function save() {
  const payload = { paramDesc: form.paramDesc, remark: form.remark };
  if (editing.value) {
    await updateRecipeVersion(props.recipeId, editing.value.id, payload);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createRecipeVersion(props.recipeId, payload);
    window.$message?.success($t('common.addSuccess'));
  }
  resetForm();
  await load();
}

async function copy(row: RecipeVersion) {
  await copyRecipeVersion(props.recipeId, row.id);
  window.$message?.success($t('common.addSuccess'));
  await load();
}

function requireActor() {
  if (!form.actor.trim()) {
    window.$message?.warning($t('page.mes.recipeVersion.actorRequired'));
    return false;
  }
  return true;
}

async function submit(row: RecipeVersion) {
  if (!requireActor()) return;
  await submitRecipeVersion(props.recipeId, row.id, { assignee: form.actor.trim() });
  window.$message?.success($t('page.mes.recipeVersion.submitted'));
  await load();
}

async function approve(row: RecipeVersion) {
  if (!requireActor()) return;
  await approveRecipeVersion(props.recipeId, row.id, { operator: form.actor.trim() });
  window.$message?.success($t('page.mes.recipeVersion.activated'));
  await load();
}

async function reject(row: RecipeVersion) {
  if (!requireActor()) return;
  await rejectRecipeVersion(props.recipeId, row.id, { operator: form.actor.trim() });
  window.$message?.success($t('page.mes.recipeVersion.rejected'));
  await load();
}

function remove(row: RecipeVersion) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.recipeVersion.deleteConfirm', { name: String(row.versionNo) }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteRecipeVersion(props.recipeId, row.id);
      window.$message?.success($t('common.deleteSuccess'));
      await load();
    }
  });
}

const columns = computed<DataTableColumns<RecipeVersion>>(() => [
  { title: $t('page.mes.recipeVersion.versionNo'), key: 'versionNo', width: 80 },
  { title: $t('page.mes.recipeVersion.status'), key: 'status', width: 110 },
  { title: $t('page.mes.recipeVersion.paramDesc'), key: 'paramDesc' },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 260,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        h(NButton, { size: 'small', quaternary: true, onClick: () => copy(row) }, () => $t('page.mes.recipeVersion.copy')),
        row.status === 'DRAFT' && hasAuth('mes:recipe:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => editRow(row) }, () => $t('common.edit'))
          : null,
        row.status === 'DRAFT' && hasAuth('mes:recipe:edit')
          ? h(NButton, { size: 'small', quaternary: true, onClick: () => submit(row) }, () => $t('page.mes.recipeVersion.submit'))
          : null,
        row.status === 'PENDING' && hasAuth('mes:recipe:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'success', onClick: () => approve(row) }, () =>
              $t('page.mes.recipeVersion.approve')
            )
          : null,
        row.status === 'PENDING' && hasAuth('mes:recipe:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => reject(row) }, () =>
              $t('page.mes.recipeVersion.reject')
            )
          : null,
        row.status === 'DRAFT' && hasAuth('mes:recipe:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => remove(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);
</script>

<template>
  <NModal v-model:show="visible" preset="card" class="w-860px" :title="`${$t('page.mes.recipeVersion.title')} ${recipeCode}`">
    <DataTable :columns="columns" :data="rows" :loading="loading" :bordered="false" size="small" />
    <NForm ref="formRef" :model="form" label-placement="left" label-width="96" class="mt-16px">
      <NFormItem :label="$t('page.mes.recipeVersion.paramDesc')" path="paramDesc">
        <NInput v-model:value="form.paramDesc" type="textarea" :rows="3" />
      </NFormItem>
      <NFormItem :label="$t('common.remark')" path="remark">
        <NInput v-model:value="form.remark" />
      </NFormItem>
      <NFormItem :label="$t('page.mes.recipeVersion.actor')">
        <NInput v-model:value="form.actor" :placeholder="$t('page.mes.recipeVersion.actorHint')" />
      </NFormItem>
      <div class="flex justify-end gap-8px">
        <NButton @click="resetForm">{{ $t('common.reset') }}</NButton>
        <NButton v-if="!editing && hasAuth('mes:recipe:add')" type="primary" @click="save">{{ $t('common.add') }}</NButton>
        <NButton v-else-if="editing && hasAuth('mes:recipe:edit')" type="primary" @click="save">{{ $t('common.save') }}</NButton>
      </div>
    </NForm>
  </NModal>
</template>
