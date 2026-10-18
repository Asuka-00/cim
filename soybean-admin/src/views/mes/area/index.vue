<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules, type SelectOption } from 'naive-ui';
import { createArea, deleteArea, getAreaList, updateArea, type Area } from '@/service/api/mes/area';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesArea' });

const { hasAuth } = useAuth();
const parents = ref<Area[]>([]);

const parentOptions = computed<SelectOption[]>(() =>
  parents.value
    .filter(item => item.id !== editing.value?.id)
    .map(item => ({ label: `${item.areaCode} ${item.areaName}`, value: item.id }))
);

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<Area, { areaCode: string }>({
    initialQuery: { areaCode: '' },
    fetcher: getAreaList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'areaCode', label: $t('page.mes.area.areaCode'), type: 'input' }
]);

const showModal = ref(false);
const editing = ref<Area | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  areaCode: '',
  areaName: '',
  parentId: null as string | null,
  remark: '',
  enabled: 1
});

const rules = computed<FormRules>(() => ({
  areaCode: [{ required: true, message: $t('page.mes.area.required'), trigger: 'blur' }],
  areaName: [{ required: true, message: $t('page.mes.area.required'), trigger: 'blur' }]
}));

function parentLabel(id?: string | null) {
  const parent = parents.value.find(item => item.id === id);
  return parent ? parent.areaCode : '';
}

function openCreate() {
  editing.value = null;
  Object.assign(form, { areaCode: '', areaName: '', parentId: null, remark: '', enabled: 1 });
  showModal.value = true;
}

function openEdit(row: Area) {
  editing.value = row;
  Object.assign(form, { ...row, parentId: row.parentId ?? null });
  showModal.value = true;
}

const columns = computed<DataTableColumns<Area>>(() => [
  { title: $t('page.mes.area.areaCode'), key: 'areaCode' },
  { title: $t('page.mes.area.areaName'), key: 'areaName' },
  { title: $t('page.mes.area.parent'), key: 'parentId', render: row => parentLabel(row.parentId) },
  {
    title: $t('common.status'),
    key: 'enabled',
    width: 90,
    render: row => (row.enabled === 1 ? $t('common.enabled') : $t('common.disabled'))
  },
  {
    title: $t('common.operate'),
    key: 'actions',
    width: 160,
    render: row =>
      h('div', { class: 'flex gap-8px' }, [
        hasAuth('mes:area:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:area:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);

async function loadParents() {
  const { data } = await getAreaList({ page: 1, pageSize: 200 });
  parents.value = data?.records ?? [];
}

async function handleSubmit() {
  await formRef.value?.validate();
  const payload = { ...form, parentId: form.parentId || null };
  if (editing.value) {
    await updateArea(String(editing.value.id), payload);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createArea(payload);
    window.$message?.success($t('common.addSuccess'));
  }
  await Promise.all([fetchData(), loadParents()]);
}

function handleDelete(row: Area) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.area.deleteConfirm', { name: row.areaCode }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteArea(String(row.id));
      window.$message?.success($t('common.deleteSuccess'));
      await Promise.all([fetchData(), loadParents()]);
    }
  });
}

onMounted(async () => {
  await Promise.all([fetchData(), loadParents()]);
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
          <span>{{ $t('page.mes.area.title') }}</span>
          <PermissionGuard code="mes:area:add">
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
      :title="editing ? $t('page.mes.area.editTitle') : $t('page.mes.area.addTitle')"
      class="w-560px"
      :on-confirm="handleSubmit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.area.areaCode')" path="areaCode">
          <NInput v-model:value="form.areaCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.area.areaName')" path="areaName">
          <NInput v-model:value="form.areaName" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.area.parent')" path="parentId">
          <NSelect v-model:value="form.parentId" :options="parentOptions" filterable clearable />
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
  </div>
</template>
