<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui';
import {
  createStepGroup,
  deleteStepGroup,
  getStepGroupList,
  updateStepGroup,
  type StepGroup
} from '@/service/api/mes/step-group';
import { usePageQuery } from '@/hooks/business/page-query';
import FilterBar, { type FilterField } from '@/components/business/filter-bar.vue';
import CrudDialog from '@/components/business/crud-dialog.vue';
import PermissionGuard from '@/components/business/permission-guard.vue';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({ name: 'MesStepGroup' });

const { hasAuth } = useAuth();

const { loading, list, total, currentPage, pageSize, searchParams, fetchData, handleSearch, handleReset, onPageUpdate, onSizeUpdate } =
  usePageQuery<StepGroup, { groupCode: string }>({
    initialQuery: { groupCode: '' },
    fetcher: getStepGroupList
  });

const filterFields = computed<FilterField[]>(() => [
  { key: 'groupCode', label: $t('page.mes.stepGroup.groupCode'), type: 'input' }
]);

const showModal = ref(false);
const editing = ref<StepGroup | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  groupCode: '',
  groupName: '',
  remark: '',
  enabled: 1
});

const rules = computed<FormRules>(() => ({
  groupCode: [{ required: true, message: $t('page.mes.stepGroup.required'), trigger: 'blur' }],
  groupName: [{ required: true, message: $t('page.mes.stepGroup.required'), trigger: 'blur' }]
}));

function openCreate() {
  editing.value = null;
  Object.assign(form, { groupCode: '', groupName: '', remark: '', enabled: 1 });
  showModal.value = true;
}

function openEdit(row: StepGroup) {
  editing.value = row;
  Object.assign(form, { groupCode: row.groupCode, groupName: row.groupName, remark: row.remark ?? '', enabled: row.enabled });
  showModal.value = true;
}

const columns = computed<DataTableColumns<StepGroup>>(() => [
  { title: $t('page.mes.stepGroup.groupCode'), key: 'groupCode' },
  { title: $t('page.mes.stepGroup.groupName'), key: 'groupName' },
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
        hasAuth('mes:step-group:edit')
          ? h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => openEdit(row) }, () => $t('common.edit'))
          : null,
        hasAuth('mes:step-group:delete')
          ? h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, () => $t('common.delete'))
          : null
      ])
  }
]);

async function handleSubmit() {
  await formRef.value?.validate();
  if (editing.value) {
    await updateStepGroup(String(editing.value.id), form);
    window.$message?.success($t('common.saveSuccess'));
  } else {
    await createStepGroup(form);
    window.$message?.success($t('common.addSuccess'));
  }
  await fetchData();
}

function handleDelete(row: StepGroup) {
  window.$dialog?.warning({
    title: $t('common.delete'),
    content: $t('page.mes.stepGroup.deleteConfirm', { name: row.groupCode }),
    positiveText: $t('common.delete'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      await deleteStepGroup(String(row.id));
      window.$message?.success($t('common.deleteSuccess'));
      await fetchData();
    }
  });
}

onMounted(() => {
  fetchData();
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
          <span>{{ $t('page.mes.stepGroup.title') }}</span>
          <PermissionGuard code="mes:step-group:add">
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
      :title="editing ? $t('page.mes.stepGroup.editTitle') : $t('page.mes.stepGroup.addTitle')"
      class="w-560px"
      :on-confirm="handleSubmit"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="96">
        <NFormItem :label="$t('page.mes.stepGroup.groupCode')" path="groupCode">
          <NInput v-model:value="form.groupCode" />
        </NFormItem>
        <NFormItem :label="$t('page.mes.stepGroup.groupName')" path="groupName">
          <NInput v-model:value="form.groupName" />
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
